package pl.janda.onepiecetcg.infrastructure.persistence;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.janda.onepiecetcg.application.OnePieceTcgApplication;
import pl.janda.onepiecetcg.application.model.CardSearchField;
import pl.janda.onepiecetcg.application.model.CardSortField;
import pl.janda.onepiecetcg.application.model.SetCard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * First Testcontainers-based test in this repo. Needed because the SEMANTIC search mode relies on a
 * Postgres-only generated tsvector column + GIN index (see scripts/db/add-card-semantic-search-vector.sql)
 * - which a mocked DSLContext can't exercise.
 *
 * JooqSetCardQueryAdapter.search()/countSearch() take (name, searchField, types, colors, rarities,
 * flatRarities, costs, power, counterAmount, attributes, attributeCombos, subTypes, prefixes,
 * sortBy, sortOrder, page, limit, showAllVariants) - 18 params for search(). Only a handful vary per
 * test below.
 *
 * OnePieceTcgApplication lives under application/, not the root package, so @SpringBootTest's
 * upward package scan can't find it - declared explicitly here.
 */
@SpringBootTest(classes = OnePieceTcgApplication.class)
@Testcontainers
class JooqSetCardQueryAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        // prepareThreshold=0 disables server-side prepared statements: without it, pgjdbc reuses a
        // cached plan for the repeated INSERT across tests and fails with "cached plan must not
        // change result type" after the schema migration (added column) runs in the same session.
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&prepareThreshold=0");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private JooqSetCardQueryAdapter adapter;

    @Autowired
    private SetCardJpaRepository jpaRepository;

    @Autowired
    private DSLContext dsl;

    private static boolean schemaMigrated = false;

    @BeforeEach
    void setUp() {
        // Hibernate (ddl-auto: update) has already created set_cards by the time this runs; apply the
        // same generated-column migration described in scripts/db/add-card-semantic-search-vector.sql.
        // Run once for the whole class (not per-test): re-running this DDL before every test on the
        // same pooled connection makes Postgres invalidate an already-prepared INSERT plan, causing
        // "cached plan must not change result type" on a later saveAllAndFlush().
        if (!schemaMigrated) {
            dsl.execute("""
                    ALTER TABLE set_cards
                        ADD COLUMN IF NOT EXISTS card_semantic_search_vector tsvector
                        GENERATED ALWAYS AS (
                            to_tsvector('simple'::regconfig,
                                coalesce(card_name, '') || ' ' ||
                                coalesce(card_type, '') || ' ' ||
                                coalesce(card_color, '') || ' ' ||
                                coalesce(card_cost, '') || ' ' ||
                                coalesce(card_power, '') || ' ' ||
                                coalesce(counter_amount::text, '') || ' ' ||
                                coalesce(attribute, '') || ' ' ||
                                coalesce(card_set_id, '') || ' ' ||
                                coalesce(sub_types, '') || ' ' ||
                                coalesce(card_text, '')
                            )
                        ) STORED
                    """);
            dsl.execute("""
                    CREATE INDEX IF NOT EXISTS idx_set_cards_card_semantic_search_vector
                        ON set_cards USING GIN (card_semantic_search_vector)
                    """);
            schemaMigrated = true;
        }

        jpaRepository.deleteAll();
        jpaRepository.saveAllAndFlush(List.of(
                SetCard.builder()
                        .cardName("Monkey D. Luffy")
                        .cardSetId("OP01-001")
                        .cardText("If you have a [Straw Hat] type Character: draw 1 card.")
                        .cardCost("1")
                        .representative(true)
                        .build(),
                SetCard.builder()
                        .cardName("Roronoa Zoro")
                        .cardSetId("OP01-002")
                        .cardText("[On Play] Give up to 1 of your Leader or Character cards +1000 power for this turn.")
                        .cardCost("3")
                        .attribute("Slash")
                        .representative(true)
                        .build(),
                SetCard.builder()
                        .cardName("Nami")
                        .cardSetId("OP01-003")
                        .cardText("[DON!!x1] This Character gains [Blocker].")
                        .cardCost("5")
                        .representative(true)
                        .build(),
                SetCard.builder()
                        .cardName("Usopp")
                        .cardSetId("OP01-004")
                        .cardText("[On Play] Look at 3 cards from the top of your deck.")
                        .cardCost("2")
                        .subTypes("Hat / Straw")
                        .representative(true)
                        .build()
        ));
    }

    @Test
    void searchField_name_matchesOnlyOnNameAndCardNumber_notOnDescriptionOnlyContent() {
        var results = adapter.search(
                "Luffy", CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false);

        assertThat(results).extracting(SetCard::getCardName).containsExactly("Monkey D. Luffy");

        // "Blocker" only appears in Nami's card text, never in a card name - NAME mode must not match it.
        var noMatch = adapter.search(
                "Blocker", CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false);
        assertThat(noMatch).isEmpty();
    }

    @Test
    void descriptionSearch_doesNotAffectDefaultSortOrder() {
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.CARD_NUMBER, null, 0, 50, false);

        assertThat(results).extracting(SetCard::getCardSetId)
                .containsExactly("OP01-001", "OP01-002", "OP01-003", "OP01-004");
    }

    @Test
    void costs_filtersToOnlyMatchingValues_whenNonEmpty() {
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, List.of(1, 5), null, null, null, null, null, null,
                null, null, 0, 50, false);

        assertThat(results).extracting(SetCard::getCardName)
                .containsExactlyInAnyOrder("Monkey D. Luffy", "Nami");
    }

    @Test
    void costs_doesNotFilter_whenNullOrEmpty() {
        var withNull = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false);
        assertThat(withNull).hasSize(4);

        var withEmpty = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, List.of(), null, null, null, null, null, null,
                null, null, 0, 50, false);
        assertThat(withEmpty).hasSize(4);
    }

    @Test
    void semanticSearch_matchesOnAttributeField() {
        // "Slash" is only set on Zoro's attribute field, never in any card's name/text - SEMANTIC
        // mode's broader tsvector (which folds in attribute) must still match it.
        var semanticResults = adapter.search(
                "Slash", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false);
        assertThat(semanticResults).extracting(SetCard::getCardName).containsExactly("Roronoa Zoro");
    }

    @Test
    void semanticSearch_quotedPhrase_matchesExactWordOrderOnly() {
        // Unquoted: any-word-order AND match - both Luffy (exact adjacent phrase) and Usopp (both
        // words present, reversed/non-adjacent) match.
        var unquoted = adapter.search(
                "Straw Hat", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false);
        assertThat(unquoted).extracting(SetCard::getCardName)
                .containsExactlyInAnyOrder("Monkey D. Luffy", "Usopp");

        // Double-quoted: exact phrase, word order preserved - only Luffy's card text has "Straw Hat"
        // as an adjacent phrase.
        var doubleQuoted = adapter.search(
                "\"Straw Hat\"", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false);
        assertThat(doubleQuoted).extracting(SetCard::getCardName).containsExactly("Monkey D. Luffy");

        // Single-quoted: same exact-phrase behavior as double-quoted.
        var singleQuoted = adapter.search(
                "'Straw Hat'", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false);
        assertThat(singleQuoted).extracting(SetCard::getCardName).containsExactly("Monkey D. Luffy");
    }
}
