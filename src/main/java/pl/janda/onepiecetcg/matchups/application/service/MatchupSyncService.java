package pl.janda.onepiecetcg.matchups.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.matchups.application.model.*;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.matchups.application.repository.*;

import java.time.LocalDateTime;
import java.util.List;
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

    private final RawDecklistRepository rawDecklistRepository;

    private final MatchupNormalizationService normalizationService;

    private final MatchupCardProfileService cardProfileService;

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
                .filter(current -> Objects.equals(current.getSourceSnapshotId(), rawSnapshot.getId()))
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

        var rawDecklists = rawDecklistRepository.findBySnapshotId(rawSnapshot.getId());
        var normalizedLeaderCards = cardProfileService.calculateProfiles(rawDecklists, validLeaderCodes);
        var leaderCards = enrichLeaderCards(normalizedLeaderCards);

        var snapshotInfo = MatchupSnapshotInfo.builder()
                .sourceSnapshotId(rawSnapshot.getId())
                .dataset(rawSnapshot.getDataset())
                .totalMatches(rawSnapshot.getTotalMatches())
                .scrapedAt(rawSnapshot.getScrapedAt())
                .syncedAt(LocalDateTime.now())
                .build();

        matchupReplacementService.replaceAll(snapshotInfo, leaders, pairs, leaderCards);

        var totalDuration = System.currentTimeMillis() - startTime;
        log.info("Matchups sync completed successfully - {} leaders, {} pairs, {} leader cards, total time: {}ms",
                leaders.size(), pairs.size(), leaderCards.size(), totalDuration);
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

    private List<MatchupLeaderCard> enrichLeaderCards(List<NormalizedLeaderCard> normalizedCards) {
        if (normalizedCards.isEmpty()) {
            return List.of();
        }
        var cardCodes = normalizedCards.stream().map(NormalizedLeaderCard::cardCode).distinct().toList();
        var cardsByCode = cardCatalogUseCase.getRepresentativeCardsByCardCodes(cardCodes).stream()
                .collect(Collectors.toMap(SetCard::getCardSetId, Function.identity()));
        return normalizedCards.stream()
                .map(card -> toMatchupLeaderCard(card, cardsByCode))
                .filter(Objects::nonNull)
                .toList();
    }

    private MatchupLeaderCard toMatchupLeaderCard(NormalizedLeaderCard normalizedCard,
                                                  Map<String, SetCard> cardsByCode) {
        var card = cardsByCode.get(normalizedCard.cardCode());
        if (card == null) {
            log.warn("Dropping matchup card '{}' for leader '{}' - no matching card found in set_cards",
                    normalizedCard.cardCode(), normalizedCard.leaderCode());
            return null;
        }
        return MatchupLeaderCard.builder()
                .leaderCode(normalizedCard.leaderCode())
                .cardCode(normalizedCard.cardCode())
                .category(normalizedCard.category())
                .name(card.getCardName())
                .imageUrl(card.getCardImage())
                .cardType(card.getCardType())
                .cost(parseIntSafe(card.getCardCost()))
                .power(parseIntSafe(card.getCardPower()))
                .counter(card.getCounterAmount())
                .effect(card.getCardText())
                .inclusionRate(normalizedCard.inclusionRate())
                .typicalCopies(normalizedCard.typicalCopies())
                .build();
    }

    private Integer parseIntSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
