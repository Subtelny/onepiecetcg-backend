package pl.janda.onepiecetcg.matchups.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;
import pl.janda.onepiecetcg.matchups.application.model.NormalizedLeaderStat;
import pl.janda.onepiecetcg.matchups.application.model.NormalizedMatchup;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupSnapshotInfoRepository;
import pl.janda.onepiecetcg.matchups.application.repository.RawLeaderStatRepository;
import pl.janda.onepiecetcg.matchups.application.repository.RawMatchupRepository;
import pl.janda.onepiecetcg.matchups.application.repository.RawMatchupSnapshotRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchupSyncService implements MatchupSyncUseCase {

    private final RawMatchupSnapshotRepository rawSnapshotRepository;

    private final RawLeaderStatRepository rawLeaderStatRepository;

    private final RawMatchupRepository rawMatchupRepository;

    private final MatchupNormalizationService normalizationService;

    private final CardCatalogUseCase cardCatalogUseCase;

    private final MatchupReplacementService matchupReplacementService;

    private final MatchupSnapshotInfoRepository matchupSnapshotInfoRepository;

    @Override
    public boolean syncMatchups() {
        var startTime = System.currentTimeMillis();
        log.info("Matchups sync started");

        var rawSnapshot = rawSnapshotRepository.findLatest().orElse(null);
        if (rawSnapshot == null) {
            log.warn("No matchmaking snapshot found in tcgmatchmaking_matchup_snapshots, skipping sync");
            return false;
        }

        var alreadySynced = matchupSnapshotInfoRepository.findCurrent()
                .filter(current -> current.getDataset().equals(rawSnapshot.getDataset()))
                .filter(current -> current.getScrapedAt().isEqual(rawSnapshot.getScrapedAt()))
                .isPresent();
        if (alreadySynced) {
            log.info("Matchup snapshot '{}' scraped at {} already synced, skipping",
                    rawSnapshot.getDataset(), rawSnapshot.getScrapedAt());
            return false;
        }

        var rawLeaderStats = rawLeaderStatRepository.findBySnapshotId(rawSnapshot.getId());
        var rawMatchups = rawMatchupRepository.findBySnapshotId(rawSnapshot.getId());

        var normalizedLeaderStats = normalizationService.normalizeAndMergeLeaderStats(rawLeaderStats);

        var cardsByCode = cardCatalogUseCase.getRepresentativeCardsByCardCodes(
                        normalizedLeaderStats.stream().map(NormalizedLeaderStat::cardCode).toList())
                .stream()
                .collect(Collectors.toMap(SetCard::getCardSetId, Function.identity()));

        var leaders = normalizedLeaderStats.stream()
                .map(stat -> toMatchupLeader(stat, cardsByCode))
                .filter(Objects::nonNull)
                .toList();

        var validLeaderCodes = leaders.stream().map(MatchupLeader::getCardCode).collect(Collectors.toSet());

        var normalizedMatchups = normalizationService.normalizeAndMergeMatchups(rawMatchups, validLeaderCodes);
        var pairs = normalizedMatchups.stream()
                .map(this::toMatchupPair)
                .toList();

        var snapshotInfo = MatchupSnapshotInfo.builder()
                .dataset(rawSnapshot.getDataset())
                .totalMatches(rawSnapshot.getTotalMatches())
                .scrapedAt(rawSnapshot.getScrapedAt())
                .syncedAt(LocalDateTime.now())
                .build();

        matchupReplacementService.replaceAll(snapshotInfo, leaders, pairs);

        var totalDuration = System.currentTimeMillis() - startTime;
        log.info("Matchups sync completed successfully - {} leaders, {} pairs, total time: {}ms",
                leaders.size(), pairs.size(), totalDuration);
        return true;
    }

    private MatchupLeader toMatchupLeader(NormalizedLeaderStat stat, Map<String, SetCard> cardsByCode) {
        var card = cardsByCode.get(stat.cardCode());
        if (card == null) {
            log.warn("Dropping leader stat for '{}' - no matching card found in set_cards", stat.cardCode());
            return null;
        }
        if (!"Leader".equalsIgnoreCase(card.getCardType())) {
            log.warn("Dropping leader stat for '{}' - resolved card type is '{}', not Leader",
                    stat.cardCode(), card.getCardType());
            return null;
        }
        return MatchupLeader.builder()
                .cardCode(stat.cardCode())
                .name(card.getCardName())
                .colors(card.getCardColor())
                .imageUrl(card.getCardImage())
                .popularity(stat.popularity())
                .matches(stat.matches())
                .winRate(stat.winRate())
                .build();
    }

    private MatchupPair toMatchupPair(NormalizedMatchup matchup) {
        return MatchupPair.builder()
                .leaderCode(matchup.leaderCode())
                .opponentCode(matchup.opponentCode())
                .games(matchup.games())
                .winRate(matchup.winRate())
                .firstWinRate(matchup.firstWinRate())
                .secondWinRate(matchup.secondWinRate())
                .firstGames(matchup.firstGames())
                .secondGames(matchup.secondGames())
                .build();
    }
}
