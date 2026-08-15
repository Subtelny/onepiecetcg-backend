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
import pl.janda.onepiecetcg.cards.application.model.*;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardSetSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jooq.JooqSetCardQueryAdapter;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa.SetCardJpaRepository;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = OnePieceTcgApplication.class)
@Testcontainers
class JooqSetCardQueryAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;
    @MockitoBean
    private CardSetSyncUseCase cardSetSyncUseCase;
    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;
    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;
    @MockitoBean
    private CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {


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

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();


        dsl.execute("DELETE FROM card_errata");
        jpaRepository.saveAllAndFlush(List.of(
                SetCard.builder()
                        .cardName("Monkey D. Luffy")
                        .cardSetId("OP01-001")
                        .cardText("If you have a [Straw Hat] type Character: draw 1 card.")
                        .cardCost("1")
                        .variantIndex("0")
                        .build(),
                SetCard.builder()
                        .cardName("Roronoa Zoro")
                        .cardSetId("OP01-002")
                        .cardText("[On Play] Give up to 1 of your Leader or Character cards +1000 power for this turn.")
                        .cardCost("3")
                        .attribute("Slash")
                        .variantIndex("0")
                        .build(),
                SetCard.builder()
                        .cardName("Nami")
                        .cardSetId("OP01-003")
                        .cardText("[DON!!x1] This Character gains [Blocker].")
                        .cardCost("5")
                        .variantIndex("0")
                        .build(),
                SetCard.builder()
                        .cardName("Usopp")
                        .cardSetId("OP01-004")
                        .cardText("[On Play] Look at 3 cards from the top of your deck.")
                        .cardCost("2")
                        .subTypes("Hat / Straw")
                        .variantIndex("0")
                        .build(),
                SetCard.builder()
                        .cardName("Nico Robin")
                        .cardSetId("OP01-005")
                        .cardText("[On Play] Reveal the top card of your deck.")
                        .cardCost("4")
                        .attribute("?")
                        .variantIndex("0")
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


        var noMatch = adapter.search(
                "Blocker", CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(noMatch).isEmpty();
    }

    @Test
    void searchField_name_matchesVariantDisplayNameAndReturnsItsMetadata() {
        jpaRepository.saveAndFlush(SetCard.builder()
                .cardName("Nami")
                .displayName("Nami (Winner)")
                .sourceProduct("Winner Pack 2026 Vol. 2")
                .cardSetId("OP01-003")
                .variantIndex("p1")
                .build());

        var results = adapter.search(
                "Winner", CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, true, false);

        assertThat(results).singleElement().satisfies(card -> {
            assertThat(card.getCardName()).isEqualTo("Nami");
            assertThat(card.getDisplayName()).isEqualTo("Nami (Winner)");
            assertThat(card.getSourceProduct()).isEqualTo("Winner Pack 2026 Vol. 2");
            assertThat(card.getVariantIndex()).isEqualTo("p1");
        });
    }

    @Test
    void representativeLookup_returnsOnlyRequestedZeroIndexVariants() {
        jpaRepository.saveAndFlush(SetCard.builder()
                .cardName("Alternate Luffy")
                .cardSetId("OP01-001")
                .variantIndex("p1")
                .build());

        var results = jpaRepository.findByCardSetIdInAndVariantIndex(List.of("OP01-001", "OP01-003"), "0");

        assertThat(results)
                .extracting(SetCard::getCardSetId)
                .containsExactlyInAnyOrder("OP01-001", "OP01-003");
        assertThat(results).allMatch(SetCard::isRepresentative);
    }

    @Test
    void variantLookup_distinguishesParallelAndReprintWithTheSameNumber() {
        jpaRepository.saveAllAndFlush(List.of(
                SetCard.builder()
                        .cardName("Parallel Luffy")
                        .cardSetId("OP01-001")
                        .variantIndex("p1")
                        .build(),
                SetCard.builder()
                        .cardName("Reprint Luffy")
                        .cardSetId("OP01-001")
                        .variantIndex("r1")
                        .build()
        ));

        var parallel = jpaRepository.findByCardSetIdAndVariantIndex("OP01-001", "p1");
        var reprint = jpaRepository.findByCardSetIdAndVariantIndex("OP01-001", "r1");

        assertThat(parallel).get().extracting(SetCard::getCardName).isEqualTo("Parallel Luffy");
        assertThat(reprint).get().extracting(SetCard::getCardName).isEqualTo("Reprint Luffy");
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


        var semanticResults = adapter.search(
                "Slash", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(semanticResults).extracting(CardSummary::getCardName).containsExactly("Roronoa Zoro");
    }

    @Test
    void semanticSearch_matchesVariantDisplayName() {
        jpaRepository.saveAndFlush(SetCard.builder()
                .cardName("Nami")
                .displayName("Nami (Winner)")
                .cardSetId("OP01-003")
                .variantIndex("p1")
                .build());

        var results = adapter.search(
                "Winner", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, true, false);

        assertThat(results).singleElement().satisfies(card -> {
            assertThat(card.getCardName()).isEqualTo("Nami");
            assertThat(card.getDisplayName()).isEqualTo("Nami (Winner)");
        });
    }

    @Test
    void semanticSearch_quotedPhrase_matchesExactWordOrderOnly() {


        var unquoted = adapter.search(
                "Straw Hat", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(unquoted).extracting(CardSummary::getCardName)
                .containsExactlyInAnyOrder("Monkey D. Luffy", "Usopp");


        var doubleQuoted = adapter.search(
                "\"Straw Hat\"", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(doubleQuoted).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");


        var singleQuoted = adapter.search(
                "'Straw Hat'", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(singleQuoted).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void semanticSearch_quotedPhraseMixedWithPlainWords_andsBothConditionsTogether() {


        var mismatched = adapter.search(
                "\"Straw Hat\" Zoro", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(mismatched).isEmpty();


        var matched = adapter.search(
                "\"Straw Hat\" Luffy", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(matched).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void semanticSearch_plainWord_matchesAsPrefixNotExactLexeme() {


        var results = adapter.search(
                "Luf", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void semanticSearch_prefixSharedAcrossRows_matchesAll() {

        var results = adapter.search(
                "Char", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactlyInAnyOrder("Monkey D. Luffy", "Roronoa Zoro", "Nami");
    }

    @Test
    void semanticSearch_prefixAndFullWord_areAndedNotOred() {


        var results = adapter.search(
                "Char Zoro", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Roronoa Zoro");
    }

    @Test
    void semanticSearch_quotedPartialWord_doesNotMatch_phraseStaysExact() {


        var results = adapter.search(
                "\"Straw Ha\"", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).isEmpty();
    }

    @Test
    void semanticSearch_prefixRemainderCombinedWithQuotedPhrase_matches() {


        var results = adapter.search(
                "\"Straw Hat\" Luf", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Monkey D. Luffy");
    }

    @Test
    void semanticSearch_punctuationOnlyQuery_stillMatchesViaIlikeFallback() {


        var results = adapter.search(
                "?", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(results).extracting(CardSummary::getCardName).containsExactly("Nico Robin");
    }

    @Test
    void attributesFilter_valueMadeOfRegexMetacharacters_isMatchedLiterally() {


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


        jpaRepository.saveAndFlush(
                SetCard.builder()
                        .cardName("Extra Card")
                        .cardSetId("OP01-000")
                        .cardText("Filler")
                        .variantIndex("0")
                        .build());

        var results = adapter.search(
                null, CardSearchField.NAME,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);

        assertThat(results.get(0).getCardName()).isEqualTo("Extra Card");
    }

    @Test
    void semanticSearch_relevanceRanking_nameMatchOutranksDescriptionOnlyMatch() {


        jpaRepository.saveAllAndFlush(List.of(
                SetCard.builder()
                        .cardName("Arlong")
                        .cardSetId("OP01-006")
                        .cardText("A fierce fish-man captain.")
                        .variantIndex("0")
                        .build(),
                SetCard.builder()
                        .cardName("Nami's Ally")
                        .cardSetId("OP01-007")
                        .cardText("If you have an [Arlong Pirates] type Character, this card gains +1000 power.")
                        .variantIndex("0")
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
    void semanticSearch_cardTypeKeyword_prioritizesActualCardTypeOverDescriptionMatch() {
        jpaRepository.saveAllAndFlush(List.of(
                SetCard.builder()
                        .cardName("Red Captain")
                        .cardSetId("OP01-006")
                        .cardType("LEADER")
                        .cardText("A fierce captain.")
                        .cardCost("9")
                        .variantIndex("0")
                        .build(),
                SetCard.builder()
                        .cardName("History Scholar")
                        .cardSetId("OP01-007")
                        .cardType("CHARACTER")
                        .cardText("Give up to 1 of your Leader cards +1000 power.")
                        .cardCost("1")
                        .variantIndex("0")
                        .build(),
                SetCard.builder()
                        .cardName("Surprise Maneuver")
                        .cardSetId("OP01-008")
                        .cardType("EVENT")
                        .cardText("Draw 1 card.")
                        .variantIndex("0")
                        .build(),
                SetCard.builder()
                        .cardName("Tactical Analyst")
                        .cardSetId("OP01-009")
                        .cardType("CHARACTER")
                        .cardText("Return up to 1 Event card to its owner's hand.")
                        .variantIndex("0")
                        .build()
        ));

        var leaderResults = adapter.search(
                "leader", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(leaderResults.getFirst().getCardName()).isEqualTo("Red Captain");

        var eventResults = adapter.search(
                "EvEnT", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, 0, 50, false, false);
        assertThat(eventResults.getFirst().getCardName()).isEqualTo("Surprise Maneuver");

        var explicitlySortedLeaderResults = adapter.search(
                "leader", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.COST, SortDirection.ASC, 0, 50, false, false);
        assertThat(explicitlySortedLeaderResults.getFirst().getCardName()).isEqualTo("History Scholar");
    }

    @Test
    void semanticSearch_withExplicitSortBy_overridesRelevanceRanking() {


        var results = adapter.search(
                "Straw Hat", CardSearchField.SEMANTIC,
                null, null, null, null, null, null, null, null, null, null, null,
                CardSortField.COST, SortDirection.DESC, 0, 50, false, false);

        assertThat(results).extracting(CardSummary::getCardName)
                .containsExactly("Usopp", "Monkey D. Luffy");
    }

}
