package pl.janda.onepiecetcg.matchups.application.service;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.matchups.application.model.RawLeaderStat;
import pl.janda.onepiecetcg.matchups.application.model.RawMatchup;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MatchupNormalizationServiceTest {

    private final MatchupNormalizationService service = new MatchupNormalizationService(new LeaderCodeNormalizer());

    @Test
    void normalizeAndMergeLeaderStats_mergesNearDuplicateRawRowsIntoOneNormalizedLeaderStat() {
        var clean = RawLeaderStat.builder()
                .leader("1xOP13-079")
                .wins(50L).losses(50L).numberOfMatches(100L)
                .popularity(BigDecimal.valueOf(30))
                .build();
        var anomaly = RawLeaderStat.builder()
                .leader("1 OP13-079 Imu")
                .wins(10L).losses(0L).numberOfMatches(10L)
                .popularity(BigDecimal.ZERO)
                .build();

        var result = service.normalizeAndMergeLeaderStats(List.of(clean, anomaly));

        assertThat(result).singleElement().satisfies(stat -> {
            assertThat(stat.cardCode()).isEqualTo("OP13-079");
            assertThat(stat.matches()).isEqualTo(110L);
            assertThat(stat.winRate()).isEqualByComparingTo("54.55");
            assertThat(stat.popularity()).isEqualByComparingTo("27.27");
        });
    }

    @Test
    void normalizeAndMergeLeaderStats_dropsUnparseableRowInsteadOfThrowing() {
        var garbage = RawLeaderStat.builder().leader("garbage").wins(1L).losses(1L).numberOfMatches(2L).build();

        var result = service.normalizeAndMergeLeaderStats(List.of(garbage));

        assertThat(result).isEmpty();
    }

    @Test
    void normalizeAndMergeMatchups_mergesDuplicatePairRowsAndRecomputesRates() {
        var first = RawMatchup.builder()
                .leader("1xOP13-079").opponent("1xOP14-020")
                .wins(3L).losses(2L).games(5L)
                .firstWinRate(BigDecimal.valueOf(60)).secondWinRate(BigDecimal.valueOf(40))
                .firstGames(2L).secondGames(3L)
                .build();
        var second = RawMatchup.builder()
                .leader("1xOP13-079").opponent("1xOP14-020")
                .wins(6L).losses(4L).games(10L)
                .firstWinRate(BigDecimal.valueOf(70)).secondWinRate(BigDecimal.valueOf(30))
                .firstGames(4L).secondGames(6L)
                .build();
        var validLeaderCodes = Set.of("OP13-079", "OP14-020");

        var result = service.normalizeAndMergeMatchups(List.of(first, second), validLeaderCodes);

        assertThat(result).singleElement().satisfies(matchup -> {
            assertThat(matchup.leaderCode()).isEqualTo("OP13-079");
            assertThat(matchup.opponentCode()).isEqualTo("OP14-020");
            assertThat(matchup.games()).isEqualTo(15L);
            assertThat(matchup.winRate()).isEqualByComparingTo("60.00");
            assertThat(matchup.firstWinRate()).isEqualByComparingTo("66.67");
            assertThat(matchup.secondWinRate()).isEqualByComparingTo("33.33");
            assertThat(matchup.firstGames()).isEqualTo(6L);
            assertThat(matchup.secondGames()).isEqualTo(9L);
        });
    }

    @Test
    void normalizeAndMergeMatchups_dropsPairWhenOpponentIsNotAValidLeaderCode() {
        var droppedPair = RawMatchup.builder()
                .leader("1xOP14-020").opponent("4xST34-003")
                .wins(1L).losses(0L).games(1L).firstGames(1L).secondGames(0L)
                .build();
        var validLeaderCodes = Set.of("OP14-020");

        var result = service.normalizeAndMergeMatchups(List.of(droppedPair), validLeaderCodes);

        assertThat(result).isEmpty();
    }
}
