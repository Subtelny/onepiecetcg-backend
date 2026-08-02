package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.service.CardErrataSyncService;
import pl.janda.onepiecetcg.cards.application.service.CardFaqSyncService;
import pl.janda.onepiecetcg.cards.application.service.CardmarketPriceSyncService;
import pl.janda.onepiecetcg.cards.application.service.CardSetSyncService;
import pl.janda.onepiecetcg.cards.application.service.SetCardSyncService;
import pl.janda.onepiecetcg.cards.application.model.CardSearchField;
import pl.janda.onepiecetcg.cards.application.model.CardSortField;
import pl.janda.onepiecetcg.cards.application.model.CardSummary;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.model.SortDirection;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * First Testcontainers-based test in this repo. Needed because the SEMANTIC search mode relies on a
 * Postgres-only generated tsvector column + GIN index (see src/main/resources/db/set-cards-search-vector.sql)
 * - which a mocked DSLContext can't exercise. That DDL is applied by the application's own
 * SetCardSearchVectorSchemaInitializer during startup, so this test does not set it up itself: the
 * search behavior asserted below is exercised against exactly the schema production boots with.
 *
 * JooqSetCardQueryAdapter.search()/countSearch() take (name, searchField, types, colors, rarities,
 * flatRarities, costs, power, counterAmount, attributes, attributeCombos, subTypes, prefixes,
 * sortBy, sortOrder, page, limit, showAllVariants, errataOnly) - 19 params for search(). Only a
 * handful vary per test below. search() returns CardSummary (the projected list-view shape), not
 * the full SetCard.
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
        // prepareThreshold=0 disables server-side prepared statements. The startup DDL adds a column to
        // set_cards, and pgjdbc otherwise risks reusing a cached plan whose result type no longer
        // matches, failing with "cached plan must not change result type".
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&prepareThreshold=0");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // The sync schedulers trigger on ApplicationReadyEvent, which @SpringBootTest publishes too. Left
    // real, they would read the source catalog on every run of this test and then delete and
    // repopulate set_cards underneath the fixtures below. Mocked out so this test owns the table.
    @MockitoBean
    private SetCardSyncService setCardSyncService;

    @MockitoBean
    private CardSetSyncService cardSetSyncService;

    @MockitoBean
    private CardErrataSyncService cardErrataSyncService;

    @MockitoBean
    private CardFaqSyncService cardFaqSyncService;

    @MockitoBean
    private CardmarketPriceSyncService cardmarketPriceSyncService;

    @Autowired
    private JooqSetCardQueryAdapter adapter;

    @Autowired
    private SetCardJpaRepository jpaRepository;

    @Autowired
    private DSLContext dsl;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
        // card_errata isn't touched by jpaRepository.deleteAll() (separate table/entity) - reset it
        // here too so errataOnly tests don't leak rows into other tests sharing this static container.
        dsl.execute("DELETE FROM card_errata");
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
                        .build(),
                SetCard.builder()
                        .cardName("Nico Robin")
                        .cardSetId("OP01-005")
                        .cardText("[On Play] Reveal the top card of your deck.")
                        .cardCost("4")
                        .attribute("?")
                        .representative(true)
                        .build()
        ));
    }

    @Test
    void searchField_name_matchesOnlyOnNameAndCardNumber_notOnDescriptionOnlyContent() {
        var results = adapter.search(
                "Luffy", CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");

        // "Blocker" only appears in Nami's card text, never in a card name - NAME mode must not match it.
        var noMatch = adapter.search(
                "Blocker", CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(noMatch).isEmpty();
    }

    @Test
    void descriptionSearch_doesNotAffectDefaultSortOrder() {
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.CARD_NUMBER, null, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardSetId)
                .containsExactly("OP01-001", "OP01-002", "OP01-003", "OP01-004", "OP01-005");
    }

    @Test
    void costs_filtersToOnlyMatchingValues_whenNonEmpty() {
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, List.of(1, 5), null, null, null, null, null, null,
                null, null, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactlyInAnyOrder("Monkey D. Luffy", "Nami");
    }

    @Test
    void costs_doesNotFilter_whenNullOrEmpty() {
        var withNull = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(withNull).hasSize(5);

        var withEmpty = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, List.of(), null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(withEmpty).hasSize(5);
    }

    @Test
    void semanticSearch_matchesOnAttributeField() {
        // "Slash" is only set on Zoro's attribute field, never in any card's name/text - SEMANTIC
        // mode's broader tsvector (which folds in attribute) must still match it.
        var semanticResults = adapter.search(
                "Slash", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(semanticResults).extracting(CardSummary::getCardName).containsExactly("Roronoa Zoro");
    }

    @Test
    void semanticSearch_quotedPhrase_matchesExactWordOrderOnly() {
        // Unquoted: any-word-order AND match - both Luffy (exact adjacent phrase) and Usopp (both
        // words present, reversed/non-adjacent) match.
        var unquoted = adapter.search(
                "Straw Hat", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(unquoted).extracting(CardSummary::getCardName)
                .containsExactlyInAnyOrder("Monkey D. Luffy", "Usopp");

        // Double-quoted: exact phrase, word order preserved - only Luffy's card text has "Straw Hat"
        // as an adjacent phrase.
        var doubleQuoted = adapter.search(
                "\"Straw Hat\"", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(doubleQuoted).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");

        // Single-quoted: same exact-phrase behavior as double-quoted.
        var singleQuoted = adapter.search(
                "'Straw Hat'", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(singleQuoted).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void semanticSearch_quotedPhraseMixedWithPlainWords_andsBothConditionsTogether() {
        // Quoted segment doesn't have to span the whole query - remaining plain words are AND-ed
        // with the exact phrase, not OR-ed. "Zoro" never appears alongside the "Straw Hat" phrase on
        // the same row, so no card can satisfy both conditions.
        var mismatched = adapter.search(
                "\"Straw Hat\" Zoro", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(mismatched).isEmpty();

        // "Luffy" (name) and the "Straw Hat" phrase (card text) both point at the same row, so the
        // AND-combined condition still matches it.
        var matched = adapter.search(
                "\"Straw Hat\" Luffy", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(matched).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void semanticSearch_plainWord_matchesAsPrefixNotExactLexeme() {
        // "Luf" is not itself a lexeme in any card's text, only a prefix of "luffy" - SEMANTIC mode
        // must match it via prefix (:*) tsquery matching, not require the exact word.
        var results = adapter.search(
                "Luf", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void semanticSearch_prefixSharedAcrossRows_matchesAll() {
        // "Char" is a prefix of "character", which appears in Luffy's, Zoro's, and Nami's card text.
        var results = adapter.search(
                "Char", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactlyInAnyOrder("Monkey D. Luffy", "Roronoa Zoro", "Nami");
    }

    @Test
    void semanticSearch_prefixAndFullWord_areAndedNotOred() {
        // "Char" (prefix of "character") appears on Luffy, Zoro, and Nami, but only Zoro's row also
        // contains "Zoro" - the two terms must be AND-ed, narrowing to just Zoro.
        var results = adapter.search(
                "Char Zoro", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Roronoa Zoro");
    }

    @Test
    void semanticSearch_quotedPartialWord_doesNotMatch_phraseStaysExact() {
        // "Straw Ha" (missing the final "t") is quoted - phraseto_tsquery still requires the exact
        // stored phrase "Straw Hat", so quoting must not gain prefix behavior.
        var results = adapter.search(
                "\"Straw Ha\"", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).isEmpty();
    }

    @Test
    void semanticSearch_prefixRemainderCombinedWithQuotedPhrase_matches() {
        // The exact phrase "Straw Hat" (Luffy's text) AND-ed with "Luf" as a plain-word remainder -
        // this only passes if prefix matching is wired through the phrase+remainder AND-combination
        // path too, not just the simple single-word path.
        var results = adapter.search(
                "\"Straw Hat\" Luf", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void semanticSearch_punctuationOnlyQuery_stillMatchesViaIlikeFallback() {
        // "?" is a real placeholder attribute value in this domain. Postgres's 'simple' parser
        // discards pure punctuation, producing an empty tsquery that can never match via "@@" -
        // emptyTsqueryFallback's ILIKE-based fallback must still surface it.
        var results = adapter.search(
                "?", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Nico Robin");
    }

    @Test
    void attributesFilter_valueMadeOfRegexMetacharacters_isMatchedLiterally() {
        // wordMatch() interpolates the value into a Postgres regex, so "?" reaches the database as a
        // quantifier with nothing to quantify unless it is escaped first - which used to abort the
        // query with "invalid regular expression: quantifier operand invalid" rather than filtering.
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, List.of("?"), null, null, null,
                null, null, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Nico Robin");
    }

    @Test
    void errataOnly_filtersToCardsWithAtLeastOneErrataRow() {
        dsl.execute("INSERT INTO card_errata (card_code, notice_date) VALUES ({0}, {1})", "OP01-001", LocalDate.now());

        var results = adapter.search(
                null, CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, true);

        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void errataOnly_combinedWithTextSearch_narrowsToIntersection() {
        // Both Luffy and Zoro have an errata row, but only Luffy's card text matches "Straw Hat" -
        // errataOnly must be AND-ed with the text search condition, not OR-ed.
        dsl.execute("INSERT INTO card_errata (card_code, notice_date) VALUES ({0}, {1})", "OP01-001", LocalDate.now());
        dsl.execute("INSERT INTO card_errata (card_code, notice_date) VALUES ({0}, {1})", "OP01-002", LocalDate.now());

        var results = adapter.search(
                "Straw Hat", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, true);

        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void errataOnly_false_doesNotFilterByErrataPresence() {
        dsl.execute("INSERT INTO card_errata (card_code, notice_date) VALUES ({0}, {1})", "OP01-001", LocalDate.now());

        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);

        assertThat(results).hasSize(5);
    }

    @Test
    void sortByCost_ascending_ordersByNumericCostNotStringCost() {
        // costs: Luffy=1, Zoro=3, Nami=5, Usopp=2, Robin=4
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.COST, SortDirection.ASC, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactly("Monkey D. Luffy", "Usopp", "Roronoa Zoro", "Nico Robin", "Nami");
    }

    @Test
    void sortByCost_descending_reversesOrder() {
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.COST, SortDirection.DESC, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactly("Nami", "Nico Robin", "Roronoa Zoro", "Usopp", "Monkey D. Luffy");
    }

    @Test
    void sortByPower_allNull_doesNotError_fallsBackToIdTiebreaker() {
        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.POWER, SortDirection.DESC, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactly("Monkey D. Luffy", "Roronoa Zoro", "Nami", "Usopp", "Nico Robin");
    }

    @Test
    void defaultSort_noExplicitSortBy_ordersByCardNumberNotInsertionId() {
        // Inserted last (highest DB id) but has the lexicographically smallest card_set_id - proves
        // the no-sortBy default is CARD_NUMBER order, not raw insertion/id order.
        jpaRepository.saveAndFlush(
                SetCard.builder()
                        .cardName("Extra Card")
                        .cardSetId("OP01-000")
                        .cardText("Filler")
                        .representative(true)
                        .build());

        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);

        assertThat(results.get(0).getCardName()).isEqualTo("Extra Card");
    }

    @Test
    void semanticSearch_relevanceRanking_nameMatchOutranksDescriptionOnlyMatch() {
        // "Arlong" is the card_name (weight A) of one card, and only appears inside another card's
        // card_text/effect (weight C) - the weighted tsvector must rank the name match first.
        jpaRepository.saveAllAndFlush(List.of(
                SetCard.builder()
                        .cardName("Arlong")
                        .cardSetId("OP01-006")
                        .cardText("A fierce fish-man captain.")
                        .representative(true)
                        .build(),
                SetCard.builder()
                        .cardName("Nami's Ally")
                        .cardSetId("OP01-007")
                        .cardText("If you have an [Arlong Pirates] type Character, this card gains +1000 power.")
                        .representative(true)
                        .build()
        ));

        var results = adapter.search(
                "Arlong", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactly("Arlong", "Nami's Ally");
    }

    @Test
    void semanticSearch_withExplicitSortBy_overridesRelevanceRanking() {
        // SEMANTIC "Straw Hat" matches Luffy (cost=1) and Usopp (cost=2) - relevance ranking would
        // put Luffy first (exact phrase match), but an explicit sortBy=COST DESC must override that.
        var results = adapter.search(
                "Straw Hat", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.COST, SortDirection.DESC, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactly("Usopp", "Monkey D. Luffy");
    }
}
