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
 * First Testcontainers-based test in this repo. Needed because the DESCRIPTION/BOTH search modes
 * rely on a Postgres-only generated tsvector column + GIN index (see
 * scripts/db/add-card-text-search-vector.sql) that a mocked DSLContext cannot exercise.
 *
 * JooqSetCardQueryAdapter.search()/countSearch() take (name, searchField, types, colors, rarities,
 * flatRarities, cost, power, counterAmount, attributes, attributeCombos, subTypes, prefixes, effects,
 * sortBy, sortOrder, page, limit) - 18 params. Only name/searchField/sortBy/page/limit vary below.
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
        // same generated-column migration described in scripts/db/add-card-text-search-vector.sql.
        // Run once for the whole class (not per-test): re-running this DDL before every test on the
        // same pooled connection makes Postgres invalidate an already-prepared INSERT plan, causing
        // "cached plan must not change result type" on a later saveAllAndFlush().
        if (!schemaMigrated) {
            dsl.execute("""
                    ALTER TABLE set_cards
                        ADD COLUMN IF NOT EXISTS card_text_search_vector tsvector
                        GENERATED ALWAYS AS (to_tsvector('simple'::regconfig, coalesce(card_text, ''))) STORED
                    """);
            dsl.execute("""
                    CREATE INDEX IF NOT EXISTS idx_set_cards_card_text_search_vector
                        ON set_cards USING GIN (card_text_search_vector)
                    """);
            schemaMigrated = true;
        }

        jpaRepository.deleteAll();
        jpaRepository.saveAllAndFlush(List.of(
                SetCard.builder()
                        .cardName("Monkey D. Luffy")
                        .cardSetId("OP01-001")
                        .cardText("If you have a [Straw Hat] type Character: draw 1 card.")
                        .representative(true)
                        .build(),
                SetCard.builder()
                        .cardName("Roronoa Zoro")
                        .cardSetId("OP01-002")
                        .cardText("[On Play] Give up to 1 of your Leader or Character cards +1000 power for this turn.")
                        .representative(true)
                        .build(),
                SetCard.builder()
                        .cardName("Nami")
                        .cardSetId("OP01-003")
                        .cardText("[DON!!x1] This Character gains [Blocker].")
                        .representative(true)
                        .build()
        ));
    }

    @Test
    void searchField_name_matchesOnlyOnNameAndCardNumber_notOnDescriptionOnlyContent() {
        var results = adapter.search(
                "Luffy", CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50);

        assertThat(results).extracting(SetCard::getCardName).containsExactly("Monkey D. Luffy");

        // "Blocker" only appears in Nami's card text, never in a card name - NAME mode must not match it.
        var noMatch = adapter.search(
                "Blocker", CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50);
        assertThat(noMatch).isEmpty();
    }

    @Test
    void searchField_description_matchesOnCardTextIncludingMultiWordPhrase() {
        var results = adapter.search(
                "Straw Hat", CardSearchField.DESCRIPTION,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50);

        assertThat(results).extracting(SetCard::getCardName).containsExactly("Monkey D. Luffy");

        // Card name "Luffy" does not appear anywhere in any card's text - DESCRIPTION mode must not match it.
        var noMatch = adapter.search(
                "Luffy", CardSearchField.DESCRIPTION,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50);
        assertThat(noMatch).isEmpty();
    }

    @Test
    void searchField_both_matchesEitherNameOrDescription() {
        var byName = adapter.search(
                "Zoro", CardSearchField.BOTH,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50);
        assertThat(byName).extracting(SetCard::getCardName).containsExactly("Roronoa Zoro");

        var byDescription = adapter.search(
                "Blocker", CardSearchField.BOTH,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50);
        assertThat(byDescription).extracting(SetCard::getCardName).containsExactly("Nami");
    }

    @Test
    void descriptionSearch_doesNotAffectDefaultSortOrder() {
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.CARD_NUMBER, null, 0, 50);

        assertThat(results).extracting(SetCard::getCardSetId)
                .containsExactly("OP01-001", "OP01-002", "OP01-003");
    }
}
