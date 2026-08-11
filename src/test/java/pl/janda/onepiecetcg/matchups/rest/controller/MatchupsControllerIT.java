package pl.janda.onepiecetcg.matchups.rest.controller;

import com.jayway.jsonpath.JsonPath;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.*;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa.SetCardJpaRepository;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OnePieceTcgApplication.class)
@AutoConfigureMockMvc
class MatchupsControllerIT extends PostgresSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private SetCardJpaRepository setCardJpaRepository;

    @Autowired
    private MatchupSyncUseCase matchupSyncUseCase;

    @MockitoBean
    private CardSetSyncUseCase cardSetSyncUseCase;

    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;

    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;

    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;

    @MockitoBean
    private CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @BeforeEach
    void setUp() {
        // tcgmatchmaking_* tables are populated by an external scraper, never by this app's own
        // startup schema - fabricate them here purely as this test's seed fixture.
        dsl.execute("""
                CREATE TABLE IF NOT EXISTS tcgmatchmaking_matchup_snapshots (
                    id BIGINT PRIMARY KEY,
                    dataset TEXT NOT NULL,
                    total_matches BIGINT NOT NULL,
                    scraped_at TIMESTAMPTZ NOT NULL
                )
                """);
        dsl.execute("""
                CREATE TABLE IF NOT EXISTS tcgmatchmaking_leader_stats (
                    snapshot_id BIGINT NOT NULL,
                    leader TEXT NOT NULL,
                    leader_group_index BIGINT NOT NULL,
                    wins BIGINT NOT NULL,
                    losses BIGINT NOT NULL,
                    number_of_matches BIGINT NOT NULL,
                    win_rate NUMERIC(5,2) NOT NULL,
                    popularity NUMERIC(5,2) NOT NULL
                )
                """);
        dsl.execute("""
                CREATE TABLE IF NOT EXISTS tcgmatchmaking_matchups (
                    snapshot_id BIGINT NOT NULL,
                    leader TEXT NOT NULL,
                    opponent TEXT NOT NULL,
                    wins BIGINT NOT NULL,
                    losses BIGINT NOT NULL,
                    games BIGINT NOT NULL,
                    win_rate NUMERIC(5,2) NOT NULL,
                    first_win_rate NUMERIC(5,2),
                    second_win_rate NUMERIC(5,2),
                    first_games BIGINT NOT NULL,
                    second_games BIGINT NOT NULL
                )
                """);
        dsl.execute("DELETE FROM tcgmatchmaking_matchups");
        dsl.execute("DELETE FROM tcgmatchmaking_leader_stats");
        dsl.execute("DELETE FROM tcgmatchmaking_matchup_snapshots");
        setCardJpaRepository.deleteAll();
    }

    @Test
    void getMatchups_afterRealSync_filtersInvalidAndDuplicateLeaderRows() throws Exception {
        setCardJpaRepository.saveAllAndFlush(List.of(
                SetCard.builder().cardSetId("OP14-020").cardName("Dracule Mihawk")
                        .cardColor("GREEN").cardImage("https://cdn.example/op14-020.png")
                        .cardType("Leader").variantIndex("0").build(),
                SetCard.builder().cardSetId("OP13-079").cardName("Charlotte Katakuri")
                        .cardColor("RED").cardImage("https://cdn.example/op13-079.png")
                        .cardType("Leader").variantIndex("0").build(),
                SetCard.builder().cardSetId("ST34-003").cardName("Imu")
                        .cardColor("PURPLE").cardType("Character").variantIndex("0").build()));

        dsl.execute("INSERT INTO tcgmatchmaking_matchup_snapshots (id, dataset, total_matches, scraped_at) " +
                        "VALUES (?, ?, ?, ?::timestamptz)",
                1L, "lw", 1000L, OffsetDateTime.now().toString());

        dsl.execute("INSERT INTO tcgmatchmaking_leader_stats " +
                        "(snapshot_id, leader, leader_group_index, wins, losses, number_of_matches, win_rate, popularity) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "1xOP14-020", 0L, 100L, 100L, 200L, new BigDecimal("50.00"), new BigDecimal("20.00"));
        dsl.execute("INSERT INTO tcgmatchmaking_leader_stats " +
                        "(snapshot_id, leader, leader_group_index, wins, losses, number_of_matches, win_rate, popularity) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "1xOP14-020", 1L, 90L, 60L, 150L, new BigDecimal("60.00"), new BigDecimal("15.00"));
        dsl.execute("INSERT INTO tcgmatchmaking_leader_stats " +
                        "(snapshot_id, leader, leader_group_index, wins, losses, number_of_matches, win_rate, popularity) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "1xOP13-079", 0L, 50L, 50L, 100L, new BigDecimal("50.00"), new BigDecimal("10.00"));
        dsl.execute("INSERT INTO tcgmatchmaking_leader_stats " +
                        "(snapshot_id, leader, leader_group_index, wins, losses, number_of_matches, win_rate, popularity) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "1 OP13-079 Imu", 0L, 10L, 0L, 10L, new BigDecimal("100.00"), new BigDecimal("0.00"));
        dsl.execute("INSERT INTO tcgmatchmaking_leader_stats " +
                        "(snapshot_id, leader, leader_group_index, wins, losses, number_of_matches, win_rate, popularity) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "4xST34-003", 0L, 1L, 2L, 3L, new BigDecimal("33.33"), new BigDecimal("0.00"));

        dsl.execute("INSERT INTO tcgmatchmaking_matchups " +
                        "(snapshot_id, leader, opponent, wins, losses, games, win_rate, " +
                        "first_win_rate, second_win_rate, first_games, second_games) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "1xOP14-020", "1xOP13-079", 5L, 5L, 10L, new BigDecimal("50.00"),
                new BigDecimal("55.00"), new BigDecimal("45.00"), 5L, 5L);
        dsl.execute("INSERT INTO tcgmatchmaking_matchups " +
                        "(snapshot_id, leader, opponent, wins, losses, games, win_rate, " +
                        "first_win_rate, second_win_rate, first_games, second_games) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                1L, "1xOP14-020", "4xST34-003", 1L, 0L, 1L, new BigDecimal("100.00"),
                null, null, 1L, 0L);

        var synced = matchupSyncUseCase.syncMatchups();
        assertThat(synced).isTrue();

        var result = mockMvc.perform(get("/api/matchups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshot.dataset").value("lw"))
                .andExpect(jsonPath("$.leaders[?(@.code=='ST34-003')]").isEmpty())
                .andExpect(jsonPath("$.leaders[?(@.code=='OP13-079')]").isNotEmpty())
                .andExpect(jsonPath("$.leaders[?(@.code=='OP14-020')]").isNotEmpty())
                .andExpect(jsonPath("$.matchups[?(@.opponentCode=='ST34-003')]").isEmpty())
                .andExpect(jsonPath("$.matchups[?(@.leaderCode=='OP14-020' && @.opponentCode=='OP13-079')]")
                        .isNotEmpty())
                // Only two real leaders exist in this fixture, so both trivially fall within the
                // top-10 cutoff - the boundary/exclusion behavior itself is unit-tested in
                // MatchupsServiceTest. This just confirms the field is populated end-to-end.
                .andExpect(jsonPath("$.topMatchups[?(@.opponentCode=='ST34-003')]").isEmpty())
                .andExpect(jsonPath("$.topMatchups[?(@.leaderCode=='OP14-020' && @.opponentCode=='OP13-079')]")
                        .isNotEmpty())
                .andReturn();

        var mergedLeader = JsonPath.<List<Map<String, Object>>>read(result.getResponse().getContentAsString(),
                "$.leaders[?(@.code=='OP13-079')]");
        assertThat(mergedLeader).hasSize(1);
        assertThat(mergedLeader.get(0).get("matches")).isEqualTo(110);
        assertThat(((Number) mergedLeader.get(0).get("winRate")).doubleValue()).isEqualTo(54.55);

        var groupedLeader = JsonPath.<List<Map<String, Object>>>read(result.getResponse().getContentAsString(),
                "$.leaders[?(@.code=='OP14-020')]");
        assertThat(groupedLeader).hasSize(1);
        assertThat(groupedLeader.get(0).get("matches")).isEqualTo(200);
        assertThat(((Number) groupedLeader.get(0).get("winRate")).doubleValue()).isEqualTo(50.0);
    }
}
