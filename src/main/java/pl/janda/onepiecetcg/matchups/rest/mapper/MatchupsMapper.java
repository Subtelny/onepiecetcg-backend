package pl.janda.onepiecetcg.matchups.rest.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.matchups.application.model.*;
import pl.janda.onepiecetcg.matchups.rest.dto.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class MatchupsMapper {

    public MatchupsResponseDto toDto(MatchupsOverview overview) {
        var cardsByLeader = overview.leaderCards().stream()
                .collect(Collectors.groupingBy(MatchupLeaderCard::getLeaderCode));
        return MatchupsResponseDto.builder()
                .snapshot(toSnapshotDto(overview.snapshot()))
                .leaders(overview.leaders().stream()
                        .map(leader -> toLeaderStatDto(leader, cardsByLeader.getOrDefault(leader.getCardCode(), List.of())))
                        .toList())
                .matchups(overview.matchups().stream().map(this::toMatchupDto).toList())
                .topMatchups(overview.topMatchups().stream().map(this::toMatchupDto).toList())
                .build();
    }

    private SnapshotDto toSnapshotDto(MatchupSnapshotInfo snapshot) {
        if (snapshot == null) {
            return null;
        }
        return SnapshotDto.builder()
                .dataset(snapshot.getDataset())
                .totalMatches(snapshot.getTotalMatches())
                .scrapedAt(snapshot.getScrapedAt() != null ? snapshot.getScrapedAt().toString() : null)
                .build();
    }

    private LeaderStatDto toLeaderStatDto(MatchupLeader leader, List<MatchupLeaderCard> cards) {
        return LeaderStatDto.builder()
                .code(leader.getCardCode())
                .name(leader.getName())
                .colors(parseColors(leader.getColors()))
                .imageUrl(leader.getImageUrl())
                .popularity(leader.getPopularity())
                .matches(leader.getMatches())
                .winRate(leader.getWinRate())
                .expectedCards(toCardDtos(cards, MatchupLeaderCardCategory.EXPECTED))
                .possibleTechs(toCardDtos(cards, MatchupLeaderCardCategory.POSSIBLE_TECH))
                .build();
    }

    private List<MatchupCardDto> toCardDtos(List<MatchupLeaderCard> cards,
                                            MatchupLeaderCardCategory category) {
        return cards.stream()
                .filter(card -> card.getCategory() == category)
                .map(this::toCardDto)
                .toList();
    }

    private MatchupCardDto toCardDto(MatchupLeaderCard card) {
        return MatchupCardDto.builder()
                .cardCode(card.getCardCode())
                .name(card.getName())
                .imageUrl(card.getImageUrl())
                .type(card.getCardType() != null ? card.getCardType().toUpperCase(Locale.ROOT) : null)
                .cost(card.getCost())
                .power(card.getPower())
                .counter(card.getCounter())
                .effect(card.getEffect())
                .inclusionRate(card.getInclusionRate())
                .typicalCopies(card.getTypicalCopies())
                .build();
    }

    private MatchupDto toMatchupDto(MatchupPair pair) {
        return MatchupDto.builder()
                .leaderCode(pair.getLeaderCode())
                .opponentCode(pair.getOpponentCode())
                .games(pair.getGames())
                .winRate(pair.getWinRate())
                .firstWinRate(pair.getFirstWinRate())
                .secondWinRate(pair.getSecondWinRate())
                .firstGames(pair.getFirstGames())
                .secondGames(pair.getSecondGames())
                .build();
    }

    private List<String> parseColors(String colors) {
        if (colors == null || colors.isBlank()) {
            return List.of();
        }
        return Arrays.stream(colors.split("\\s+"))
                .map(String::toUpperCase)
                .toList();
    }
}
