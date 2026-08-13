package pl.janda.onepiecetcg.matchups.application.service;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCardCategory;
import pl.janda.onepiecetcg.matchups.application.model.RawDecklist;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MatchupCardProfileServiceTest {

    private final MatchupCardProfileService service =
            new MatchupCardProfileService(new LeaderCodeNormalizer());

    @Test
    void calculateProfiles_weightsInclusionAndCopiesByGamesAndExcludesTheLeader() {
        var rawDecklists = List.of(
                RawDecklist.builder()
                        .leader("1xOP14-020")
                        .deck("{1xOP14-020,4xOP01-001,2xP-001,9xOP16-042,1xOP16-042}")
                        .games(80L)
                        .winRate(new BigDecimal("52.00"))
                        .build(),
                RawDecklist.builder()
                        .leader("1xOP14-020")
                        .deck("{1xOP14-020,4xOP01-001,3xOP01-003}")
                        .games(20L)
                        .winRate(new BigDecimal("45.00"))
                        .build());

        var result = service.calculateProfiles(rawDecklists, Set.of("OP14-020"));

        assertThat(result).extracting(card -> card.cardCode())
                .containsExactly("OP01-001", "OP16-042", "P-001", "OP01-003");
        assertThat(result).noneMatch(card -> card.cardCode().equals("OP14-020"));

        assertThat(result).filteredOn(card -> card.cardCode().equals("OP01-001"))
                .singleElement()
                .satisfies(card -> {
                    assertThat(card.category()).isEqualTo(MatchupLeaderCardCategory.EXPECTED);
                    assertThat(card.inclusionRate()).isEqualByComparingTo(new BigDecimal("100.00"));
                    assertThat(card.typicalCopies()).isEqualByComparingTo(new BigDecimal("4.0"));
                });
        assertThat(result).filteredOn(card -> card.cardCode().equals("OP16-042"))
                .singleElement()
                .satisfies(card -> {
                    assertThat(card.category()).isEqualTo(MatchupLeaderCardCategory.EXPECTED);
                    assertThat(card.inclusionRate()).isEqualByComparingTo(new BigDecimal("80.00"));
                    assertThat(card.typicalCopies()).isEqualByComparingTo(new BigDecimal("10.0"));
                });
        assertThat(result).filteredOn(card -> card.cardCode().equals("OP01-003"))
                .singleElement()
                .satisfies(card -> {
                    assertThat(card.category()).isEqualTo(MatchupLeaderCardCategory.POSSIBLE_TECH);
                    assertThat(card.inclusionRate()).isEqualByComparingTo(new BigDecimal("20.00"));
                    assertThat(card.typicalCopies()).isEqualByComparingTo(new BigDecimal("3.0"));
                });
    }

    @Test
    void calculateProfiles_ignoresUnknownLeadersAndCardsBelowTenPercent() {
        var rawDecklists = List.of(
                RawDecklist.builder().leader("1xOP14-020")
                        .deck("{1xOP14-020,4xOP01-001}").games(95L)
                        .winRate(new BigDecimal("50.00")).build(),
                RawDecklist.builder().leader("1xOP14-020")
                        .deck("{1xOP14-020,1xOP01-099}").games(5L)
                        .winRate(new BigDecimal("50.00")).build(),
                RawDecklist.builder().leader("1xOP99-999")
                        .deck("{1xOP99-999,4xOP01-050}").games(100L)
                        .winRate(new BigDecimal("50.00")).build());

        var result = service.calculateProfiles(rawDecklists, Set.of("OP14-020"));

        assertThat(result).extracting(card -> card.cardCode()).containsExactly("OP01-001");
    }

    @Test
    void calculateProfiles_capsBothGroupsToKeepTheMatchupsPayloadFocused() {
        var expectedCards = "4xOP01-001,4xOP01-002,4xOP01-003,4xOP01-004,4xOP01-005," +
                "4xOP01-006,4xOP01-007,4xOP01-008,4xOP01-009,4xOP01-010";
        var possibleTechs = "1xOP02-001,1xOP02-002,1xOP02-003,1xOP02-004,1xOP02-005," +
                "1xOP02-006,1xOP02-007";
        var rawDecklists = List.of(
                RawDecklist.builder().leader("1xOP14-020")
                        .deck("{" + expectedCards + "," + possibleTechs + "}").games(50L)
                        .winRate(new BigDecimal("50.00")).build(),
                RawDecklist.builder().leader("1xOP14-020")
                        .deck("{" + expectedCards + "}").games(50L)
                        .winRate(new BigDecimal("50.00")).build());

        var result = service.calculateProfiles(rawDecklists, Set.of("OP14-020"));

        assertThat(result).filteredOn(card -> card.category() == MatchupLeaderCardCategory.EXPECTED).hasSize(8);
        assertThat(result).filteredOn(card -> card.category() == MatchupLeaderCardCategory.POSSIBLE_TECH).hasSize(5);
    }

    @Test
    void calculateProfiles_usesTheLeaderSpecificUpperQuartileWhenItContainsEnoughDecklists() {
        var highPerforming = IntStream.range(0, 5)
                .mapToObj(index -> RawDecklist.builder().leader("1xOP14-020")
                        .deck("{1xOP14-020,4xOP01-001}").games(50L)
                        .winRate(BigDecimal.valueOf(70 - index)).build())
                .toList();
        var remaining = IntStream.range(0, 15)
                .mapToObj(index -> RawDecklist.builder().leader("1xOP14-020")
                        .deck("{1xOP14-020,4xOP01-099}").games(100L)
                        .winRate(BigDecimal.valueOf(55 - index)).build())
                .toList();
        var rawDecklists = new ArrayList<RawDecklist>();
        rawDecklists.addAll(highPerforming);
        rawDecklists.addAll(remaining);

        var result = service.calculateProfiles(rawDecklists, Set.of("OP14-020"));

        assertThat(result).singleElement().satisfies(card -> {
            assertThat(card.cardCode()).isEqualTo("OP01-001");
            assertThat(card.inclusionRate()).isEqualByComparingTo(new BigDecimal("100.00"));
        });
    }

    @Test
    void calculateProfiles_keepsAllDecklistsWhenTheUpperQuartileIsTooSmall() {
        var highPerforming = IntStream.range(0, 3)
                .mapToObj(index -> RawDecklist.builder().leader("1xOP14-020")
                        .deck("{1xOP14-020,4xOP01-001}").games(50L)
                        .winRate(BigDecimal.valueOf(70 - index)).build())
                .toList();
        var remaining = IntStream.range(0, 9)
                .mapToObj(index -> RawDecklist.builder().leader("1xOP14-020")
                        .deck("{1xOP14-020,4xOP01-099}").games(50L)
                        .winRate(BigDecimal.valueOf(55 - index)).build())
                .toList();
        var rawDecklists = new ArrayList<RawDecklist>();
        rawDecklists.addAll(highPerforming);
        rawDecklists.addAll(remaining);

        var result = service.calculateProfiles(rawDecklists, Set.of("OP14-020"));

        assertThat(result).extracting(card -> card.cardCode()).containsExactly("OP01-099", "OP01-001");
        assertThat(result).filteredOn(card -> card.cardCode().equals("OP01-099"))
                .singleElement()
                .satisfies(card -> assertThat(card.inclusionRate()).isEqualByComparingTo("75.00"));
    }
}
