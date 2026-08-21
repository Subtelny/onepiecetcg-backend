package pl.janda.onepiecetcg.matchups.rest.controller;

import com.jayway.jsonpath.JsonPath;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardSetSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa.SetCardJpaRepository;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.matchups.infrastructure.persistence.initializer.MatchupSchemaInitializer;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Autowired
    private MatchupSchemaInitializer matchupSchemaInitializer;

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
        dsl.execute("""
                CREATE TABLE IF NOT EXISTS tcgmatchmaking_decklists (
                    snapshot_id BIGINT NOT NULL,
                    leader TEXT NOT NULL,
                    deck TEXT[] NOT NULL,
                    games BIGINT NOT NULL,
                    win_rate NUMERIC(5,2) NOT NULL
                )
                """);
        dsl.execute("DELETE FROM tcgmatchmaking_decklists");
        dsl.execute("DELETE FROM tcgmatchmaking_matchups");
        dsl.execute("DELETE FROM tcgmatchmaking_leader_stats");
        dsl.execute("DELETE FROM tcgmatchmaking_matchup_snapshots");
        dsl.execute("DELETE FROM matchup_leader_cards");
        dsl.execute("DELETE FROM matchup_pairs");
        dsl.execute("DELETE FROM matchup_leaders");
        dsl.execute("DELETE FROM matchup_snapshot_info");
        setCardJpaRepository.deleteAll();
    }

    @Test
    void syncRetainsLatestSnapshotForEveryDatasetAndApiCanSelectEitherOne() throws Exception {
        setCardJpaRepository.saveAndFlush(SetCard.builder()
                .cardSetId("OP14-020")
                .cardName("Dracule Mihawk")
                .cardColor("GREEN, BLUE")
                .cardImage("https://cdn.example/op14-020.png")
                .cardType("Leader")
                .variantIndex("0")
                .build());

        var lwScrapedAt = OffsetDateTime.parse("2026-08-20T10:00:00Z");
        var specialQueueScrapedAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        insertSnapshot(10L, "lw", 1000L, lwScrapedAt);
        insertSnapshot(11L, "lw", 900L, lwScrapedAt.minusDays(1));
        insertSnapshot(20L, "Special_Queue", 2000L, specialQueueScrapedAt);
        insertLeaderStat(10L, 100L, new BigDecimal("40.00"), new BigDecimal("10.00"));
        insertLeaderStat(11L, 90L, new BigDecimal("30.00"), new BigDecimal("9.00"));
        insertLeaderStat(20L, 200L, new BigDecimal("60.00"), new BigDecimal("20.00"));

        assertThat(matchupSyncUseCase.syncMatchups()).isTrue();

        assertThat(dsl.fetchCount(dsl.selectFrom(org.jooq.impl.DSL.table("matchup_snapshot_info")))).isEqualTo(2);
        assertThat(dsl.fetchCount(dsl.selectFrom(org.jooq.impl.DSL.table("matchup_leaders")))).isEqualTo(2);

        mockMvc.perform(get("/api/matchups/overview").queryParam("dataset", "lw"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshot.dataset").value("lw"))
                .andExpect(jsonPath("$.leaders[0].matches").value(100))
                .andExpect(jsonPath("$.leaders[0].winRate").value(40.0));

        mockMvc.perform(get("/api/matchups/overview").queryParam("dataset", "special_queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshot.dataset").value("Special_Queue"))
                .andExpect(jsonPath("$.leaders[0].matches").value(200))
                .andExpect(jsonPath("$.leaders[0].winRate").value(60.0));

        mockMvc.perform(get("/api/matchups/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshot.dataset").value("Special_Queue"));

        mockMvc.perform(get("/api/matchups/datasets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dataset").value("Special_Queue"))
                .andExpect(jsonPath("$[1].dataset").value("lw"));

        mockMvc.perform(get("/api/matchups/overview").queryParam("dataset", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void schemaMigrationAssignsTheExistingSnapshotDatasetToLegacyRows() throws Exception {
        dsl.execute("INSERT INTO matchup_snapshot_info " +
                        "(source_snapshot_id, dataset, total_matches, scraped_at, synced_at, card_profile_version) " +
                        "VALUES (?, ?, ?, ?::timestamptz, CURRENT_TIMESTAMP, ?)",
                1L, "lw", 1000L, OffsetDateTime.parse("2026-08-20T10:00:00Z").toString(), 4);

        dsl.execute("ALTER TABLE matchup_leader_cards DROP CONSTRAINT matchup_leader_cards_pkey");
        dsl.execute("ALTER TABLE matchup_leader_cards DROP COLUMN dataset");
        dsl.execute("ALTER TABLE matchup_leader_cards ADD PRIMARY KEY (leader_code, card_code)");
        dsl.execute("ALTER TABLE matchup_pairs DROP CONSTRAINT matchup_pairs_pkey");
        dsl.execute("ALTER TABLE matchup_pairs DROP COLUMN dataset");
        dsl.execute("ALTER TABLE matchup_pairs ADD PRIMARY KEY (leader_code, opponent_code)");
        dsl.execute("ALTER TABLE matchup_leaders DROP CONSTRAINT matchup_leaders_pkey");
        dsl.execute("ALTER TABLE matchup_leaders DROP COLUMN dataset");
        dsl.execute("ALTER TABLE matchup_leaders ADD PRIMARY KEY (card_code)");

        dsl.execute("INSERT INTO matchup_leaders (card_code, name, popularity, matches, win_rate) " +
                        "VALUES (?, ?, ?, ?, ?)",
                "OP14-020", "Dracule Mihawk", new BigDecimal("10.00"), 100L, new BigDecimal("50.00"));
        dsl.execute("INSERT INTO matchup_pairs " +
                        "(leader_code, opponent_code, games, win_rate, first_games, second_games) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                "OP14-020", "OP13-079", 10L, new BigDecimal("50.00"), 5L, 5L);
        dsl.execute("INSERT INTO matchup_leader_cards " +
                        "(leader_code, card_code, category, name, inclusion_rate, typical_copies) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                "OP14-020", "OP01-001", "EXPECTED", "Expected Character",
                new BigDecimal("100.00"), new BigDecimal("4.0"));

        matchupSchemaInitializer.apply();

        assertThat(dsl.fetchSingle("SELECT dataset FROM matchup_leaders").get("dataset", String.class))
                .isEqualTo("lw");
        assertThat(dsl.fetchSingle("SELECT dataset FROM matchup_pairs").get("dataset", String.class))
                .isEqualTo("lw");
        assertThat(dsl.fetchSingle("SELECT dataset FROM matchup_leader_cards").get("dataset", String.class))
                .isEqualTo("lw");
    }

    @Test
    void getMatchups_afterRealSync_filtersInvalidAndDuplicateLeaderRows() throws Exception {
        var catalogCards = new ArrayList<>(List.of(
                SetCard.builder().cardSetId("OP14-020").cardName("Dracule Mihawk")
                        .cardColor("GREEN, BLUE").cardImage("https://cdn.example/op14-020.png")
                        .cardType("Leader").variantIndex("0").build(),
                SetCard.builder().cardSetId("OP13-079").cardName("Charlotte Katakuri")
                        .cardColor("RED").cardImage("https://cdn.example/op13-079.png")
                        .cardType("Leader").variantIndex("0").build(),
                SetCard.builder().cardSetId("ST34-003").cardName("Imu")
                        .cardColor("PURPLE").cardType("Character").variantIndex("0").build(),
                SetCard.builder().cardSetId("OP01-001").cardName("Expected Character")
                        .cardType("Character").cardCost("4").cardPower("5000").counterAmount(1000)
                        .cardText("[On Play] Draw 1 card.").cardImage("https://cdn.example/op01-001.png")
                        .variantIndex("0").build(),
                SetCard.builder().cardSetId("OP01-002").cardName("Possible Tech")
                        .cardType("Event").cardCost("2").counterAmount(2000)
                        .cardText("[Counter] Up to 1 Leader gains +3000 power.")
                        .cardImage("https://cdn.example/op01-002.png").variantIndex("0").build()));
        IntStream.rangeClosed(1, 11).forEach(index -> catalogCards.add(SetCard.builder()
                .cardSetId("OP02-" + String.format("%03d", index))
                .cardName("Top Deck Card " + index)
                .cardType("Character")
                .cardCost("3")
                .counterAmount(1000)
                .cardText(index == 1 ? "[Rush]" : "")
                .variantIndex("0")
                .build()));
        setCardJpaRepository.saveAllAndFlush(catalogCards);

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

        for (var index = 0; index < 5; index++) {
            var remainingTopDeckCards = IntStream.rangeClosed(1, 11)
                    .mapToObj(cardIndex -> "'4xOP02-" + String.format("%03d", cardIndex) + "'")
                    .collect(Collectors.joining(", "));
            var deck = index == 0
                    ? "ARRAY['1xOP14-020', '4xOP01-001', '2xOP01-002', " + remainingTopDeckCards + "]::text[]"
                    : "ARRAY['1xOP14-020', '4xOP01-001']::text[]";
            dsl.execute("INSERT INTO tcgmatchmaking_decklists (snapshot_id, leader, deck, games, win_rate) " +
                            "VALUES (?, ?, " + deck + ", ?, ?)",
                    1L, "1xOP14-020", 20L, BigDecimal.valueOf(70 - index));
        }
        for (var index = 0; index < 15; index++) {
            dsl.execute("INSERT INTO tcgmatchmaking_decklists (snapshot_id, leader, deck, games, win_rate) " +
                            "VALUES (?, ?, ARRAY['1xOP14-020', '4xOP01-099']::text[], ?, ?)",
                    1L, "1xOP14-020", 100L, BigDecimal.valueOf(55 - index));
        }
        dsl.execute("INSERT INTO tcgmatchmaking_decklists (snapshot_id, leader, deck, games, win_rate) " +
                        "VALUES (?, ?, ARRAY['1xOP13-079', '4xOP01-001', '1xOP01-002']::text[], ?, ?)",
                1L, "1xOP13-079", 10L, new BigDecimal("50.00"));

        // Reproduce an upgraded installation whose Hibernate-generated check predates OBSERVED.
        dsl.execute("DELETE FROM matchup_leader_cards");
        dsl.execute("ALTER TABLE matchup_leader_cards " +
                "DROP CONSTRAINT IF EXISTS matchup_leader_cards_category_check");
        dsl.execute("""
                ALTER TABLE matchup_leader_cards
                ADD CONSTRAINT matchup_leader_cards_category_check
                CHECK (category IN ('EXPECTED', 'POSSIBLE_TECH', 'TOP_DECK_ONLY'))
                """);
        matchupSchemaInitializer.apply();

        var synced = matchupSyncUseCase.syncMatchups();
        assertThat(synced).isTrue();

        var overviewResult = mockMvc.perform(get("/api/matchups/overview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, public"))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.snapshot.dataset").value("lw"))
                .andExpect(jsonPath("$.leaders[?(@.code=='OP14-020')]").isNotEmpty())
                .andExpect(jsonPath("$.leaders[0].expectedCards").doesNotExist())
                .andExpect(jsonPath("$.matchups").doesNotExist())
                .andExpect(jsonPath("$.topMatchups[?(@.leaderCode=='OP14-020' && @.opponentCode=='OP13-079')]")
                        .isNotEmpty())
                .andReturn();

        mockMvc.perform(get("/api/matchups/overview")
                        .header(HttpHeaders.IF_NONE_MATCH, overviewResult.getResponse().getHeader(HttpHeaders.ETAG)))
                .andExpect(status().isNotModified());

        mockMvc.perform(get("/api/matchups/leaders/op14-020"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, public"))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.leader.code").value("OP14-020"))
                .andExpect(jsonPath("$.leader.expectedCards").isNotEmpty())
                .andExpect(jsonPath("$.matchups[?(@.leaderCode=='OP14-020' && @.opponentCode=='OP13-079')]")
                        .isNotEmpty());

        mockMvc.perform(get("/api/matchups/leaders/UNKNOWN"))
                .andExpect(status().isNotFound());

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
        assertThat(mergedLeader.get(0).get("profileDecklists")).isEqualTo(1);
        assertThat((List<?>) mergedLeader.get(0).get("expectedCards")).isEmpty();
        @SuppressWarnings("unchecked")
        var smallSampleTechs = (List<Map<String, Object>>) mergedLeader.get(0).get("possibleTechs");
        assertThat(smallSampleTechs).singleElement()
                .satisfies(card -> assertThat(card.get("cardCode")).isEqualTo("OP01-002"));
        @SuppressWarnings("unchecked")
        var observedCards = (List<Map<String, Object>>) mergedLeader.get(0).get("observedCards");
        assertThat(observedCards).singleElement()
                .satisfies(card -> assertThat(card.get("cardCode")).isEqualTo("OP01-001"));

        var groupedLeader = JsonPath.<List<Map<String, Object>>>read(result.getResponse().getContentAsString(),
                "$.leaders[?(@.code=='OP14-020')]");
        assertThat(groupedLeader).hasSize(1);
        assertThat(groupedLeader.get(0).get("colors")).isEqualTo(List.of("GREEN", "BLUE"));
        assertThat(groupedLeader.get(0).get("matches")).isEqualTo(200);
        assertThat(((Number) groupedLeader.get(0).get("winRate")).doubleValue()).isEqualTo(50.0);
        assertThat(groupedLeader.get(0).get("profileDecklists")).isEqualTo(5);
        assertThat((List<?>) groupedLeader.get(0).get("observedCards")).isEmpty();

        @SuppressWarnings("unchecked")
        var expectedCards = (List<Map<String, Object>>) groupedLeader.get(0).get("expectedCards");
        assertThat(expectedCards).singleElement().satisfies(card -> {
            assertThat(card.get("cardCode")).isEqualTo("OP01-001");
            assertThat(card.get("type")).isEqualTo("CHARACTER");
            assertThat(((Number) card.get("inclusionRate")).doubleValue()).isEqualTo(100.0);
            assertThat(((Number) card.get("typicalCopies")).doubleValue()).isEqualTo(4.0);
        });

        @SuppressWarnings("unchecked")
        var possibleTechs = (List<Map<String, Object>>) groupedLeader.get(0).get("possibleTechs");
        assertThat(possibleTechs).filteredOn(card -> card.get("cardCode").equals("OP01-002"))
                .singleElement().satisfies(card -> {
            assertThat(card.get("cardCode")).isEqualTo("OP01-002");
            assertThat(card.get("type")).isEqualTo("EVENT");
            assertThat(((Number) card.get("inclusionRate")).doubleValue()).isEqualTo(20.0);
            assertThat(((Number) card.get("typicalCopies")).doubleValue()).isEqualTo(2.0);
        });

        @SuppressWarnings("unchecked")
        var topDeck = (Map<String, Object>) groupedLeader.get(0).get("topDeck");
        assertThat(topDeck.get("totalCards")).isEqualTo(50);
        assertThat(topDeck.get("games")).isEqualTo(20);
        assertThat(((Number) topDeck.get("winRate")).doubleValue()).isEqualTo(70.0);
        @SuppressWarnings("unchecked")
        var topDeckCards = (List<Map<String, Object>>) topDeck.get("cards");
        assertThat(topDeckCards).hasSize(13);
        assertThat(topDeckCards.stream().mapToInt(card -> ((Number) card.get("copies")).intValue()).sum())
                .isEqualTo(50);
    }

    private void insertSnapshot(long id, String dataset, long totalMatches, OffsetDateTime scrapedAt) {
        dsl.execute("INSERT INTO tcgmatchmaking_matchup_snapshots (id, dataset, total_matches, scraped_at) " +
                        "VALUES (?, ?, ?, ?::timestamptz)",
                id, dataset, totalMatches, scrapedAt.toString());
    }

    private void insertLeaderStat(long snapshotId, long matches, BigDecimal winRate, BigDecimal popularity) {
        var wins = Math.round(matches * winRate.doubleValue() / 100.0);
        dsl.execute("INSERT INTO tcgmatchmaking_leader_stats " +
                        "(snapshot_id, leader, leader_group_index, wins, losses, number_of_matches, win_rate, popularity) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                snapshotId, "1xOP14-020", 0L, wins, matches - wins, matches, winRate, popularity);
    }
}
