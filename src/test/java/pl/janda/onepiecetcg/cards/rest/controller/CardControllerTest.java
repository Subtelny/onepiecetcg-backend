package pl.janda.onepiecetcg.cards.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.model.*;
import pl.janda.onepiecetcg.cards.application.port.in.*;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.model.PriceHistoryPoint;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.model.PriceSource;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.port.in.PriceQueryUseCase;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OnePieceTcgApplication.class)
@AutoConfigureMockMvc
class CardControllerTest extends PostgresSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardCatalogUseCase cardCatalogUseCase;

    @MockitoBean
    private CardDetailsUseCase cardDetailsUseCase;

    @MockitoBean
    private CardErrataQueryUseCase cardErrataQueryUseCase;

    @MockitoBean
    private CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @MockitoBean
    private PriceQueryUseCase priceQueryUseCase;

    @MockitoBean
    private CardSetSyncUseCase cardSetSyncUseCase;

    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;

    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;

    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;

    @MockitoBean
    private MatchupSyncUseCase matchupSyncUseCase;

    @Test
    void searchCards_deserializesSearchInEnumAndForwardsToService() throws Exception {
        when(cardCatalogUseCase.searchCards(any(CardSearchQuery.class)))
                .thenReturn(new PagedCards(List.of(), 0, 0, 50));

        mockMvc.perform(get("/api/cards").param("name", "foo").param("searchIn", "SEMANTIC"))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(CardSearchQuery.class);
        verify(cardCatalogUseCase).searchCards(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo("foo");
        assertThat(captor.getValue().searchField()).isEqualTo(CardSearchField.SEMANTIC);
    }

    @Test
    void searchCards_omittingSearchInLeavesItNullForServiceToDefault() throws Exception {
        when(cardCatalogUseCase.searchCards(any(CardSearchQuery.class)))
                .thenReturn(new PagedCards(List.of(), 0, 0, 50));

        mockMvc.perform(get("/api/cards").param("name", "Luffy"))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(CardSearchQuery.class);
        verify(cardCatalogUseCase).searchCards(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo("Luffy");
        assertThat(captor.getValue().searchField()).isNull();
    }

    @Test
    void searchCards_returnsVariantDisplayMetadata() throws Exception {
        var summary = CardSummary.builder()
                .id(1L)
                .cardSetId("OP16-079")
                .cardName("Nami")
                .displayName("Nami (Winner)")
                .sourceProduct("Winner Pack 2026 Vol. 2")
                .released(false)
                .releaseDate(LocalDate.of(2026, 8, 28))
                .variantIndex("r1")
                .build();
        when(cardCatalogUseCase.searchCards(any(CardSearchQuery.class)))
                .thenReturn(new PagedCards(List.of(summary), 1, 0, 50));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].name").value("Nami"))
                .andExpect(jsonPath("$.cards[0].displayName").value("Nami (Winner)"))
                .andExpect(jsonPath("$.cards[0].sourceProduct").value("Winner Pack 2026 Vol. 2"))
                .andExpect(jsonPath("$.cards[0].released").value(false))
                .andExpect(jsonPath("$.cards[0].releaseDate").value("2026-08-28"))
                .andExpect(jsonPath("$.cards[0].variantIndex").value("r1"));
    }

    private static PriceQuote priceQuote(String priceReference) {
        return PriceQuote.builder()
                .priceReference(priceReference)
                .source(PriceSource.CARDMARKET)
                .currency("EUR")
                .externalProductId("767954")
                .productName("Kouzuki Oden (EB01-001)")
                .averagePrice(new BigDecimal("44.00"))
                .lowPrice(new BigDecimal("37.99"))
                .trendPrice(new BigDecimal("45.19"))
                .observedAt(OffsetDateTime.parse("2026-08-15T04:00:00+02:00"))
                .build();
    }

    @Test
    void getCardByCode_forwardsTextVariantIndex() throws Exception {
        var card = SetCard.builder().id(1L).cardSetId("OP16-079").variantIndex("r1").build();
        when(cardDetailsUseCase.getCardByCode("OP16-079", "r1"))
                .thenReturn(new CardDetails(card, List.of(), List.of()));

        mockMvc.perform(get("/api/cards/by-code")
                        .param("cardCode", "OP16-079")
                        .param("variant", "r1"))
                .andExpect(status().isOk());

        verify(cardDetailsUseCase).getCardByCode("OP16-079", "r1");
    }

    @Test
    void getAllErrata_returnsErrataHistoryShapedForFrontend() throws Exception {
        var errata = CardErrata.builder()
                .cardCode("OP13-119")
                .cardName("Charlotte Katakuri")
                .scopeNote("Also applies to parallel card version.")
                .beforeText("Old text")
                .afterText("New text")
                .noticeDate(LocalDate.of(2024, 3, 3))
                .sourceUrl("https://en.onepiece-cardgame.com/rules/errata_card/#errata_1")
                .build();
        when(cardErrataQueryUseCase.listAll()).thenReturn(List.of(errata));

        mockMvc.perform(get("/api/cards/errata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardCode").value("OP13-119"))
                .andExpect(jsonPath("$[0].cardName").value("Charlotte Katakuri"))
                .andExpect(jsonPath("$[0].afterText").value("New text"))
                .andExpect(jsonPath("$[0].noticeDate").value("2024-03-03"));
    }

    @Test
    void getCardById_embedsFullErrataHistoryOrderedOldestToNewest() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("OP13-119")
                .cardName("Charlotte Katakuri")
                .cardColor("Red, Green")
                .attribute("Slash, Special")
                .build();
        var older = CardErrata.builder()
                .cardCode("OP13-119")
                .scopeNote("Also applies to parallel card version.")
                .beforeText("Old text")
                .afterText("Middle text")
                .noticeDate(LocalDate.of(2023, 6, 1))
                .build();
        var newer = CardErrata.builder()
                .cardCode("OP13-119")
                .beforeText("Middle text")
                .afterText("New text")
                .noticeDate(LocalDate.of(2024, 3, 3))
                .build();
        when(cardDetailsUseCase.getCardById("1"))
                .thenReturn(new CardDetails(card, List.of(older, newer), List.of()));

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errata.length()").value(2))
                .andExpect(jsonPath("$.errata[0].date").value("2023-06-01"))
                .andExpect(jsonPath("$.errata[0].after").value("Middle text"))
                .andExpect(jsonPath("$.errata[0].note").value("Also applies to parallel card version."))
                .andExpect(jsonPath("$.errata[1].date").value("2024-03-03"))
                .andExpect(jsonPath("$.errata[1].after").value("New text"))
                .andExpect(jsonPath("$.color[0]").value("RED"))
                .andExpect(jsonPath("$.color[1]").value("GREEN"))
                .andExpect(jsonPath("$.attribute[0]").value("Slash"))
                .andExpect(jsonPath("$.attribute[1]").value("Special"));
    }

    @Test
    void searchCards_embedsLatestPricesForEveryReturnedPrint() throws Exception {
        var summary = CardSummary.builder()
                .id(1L)
                .cardSetId("EB01-001")
                .cardName("Kouzuki Oden")
                .priceReference("single:EB01-001_p1")
                .build();
        var price = priceQuote("single:EB01-001_p1");
        when(cardCatalogUseCase.searchCards(any(CardSearchQuery.class)))
                .thenReturn(new PagedCards(List.of(summary), 1, 0, 50));
        when(priceQueryUseCase.getLatestPricesByReferences(List.of("single:EB01-001_p1")))
                .thenReturn(Map.of("single:EB01-001_p1", List.of(price)));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].prices[0].source").value("CARDMARKET"))
                .andExpect(jsonPath("$.cards[0].prices[0].currency").value("EUR"))
                .andExpect(jsonPath("$.cards[0].prices[0].productId").value("767954"))
                .andExpect(jsonPath("$.cards[0].prices[0].lowPrice").value(37.99))
                .andExpect(jsonPath("$.cards[0].prices[0].trendPrice").value(45.19))
                .andExpect(jsonPath("$.cards[0].prices[0].observedAt")
                        .value("2026-08-15T04:00:00+02:00"));
    }

    @Test
    void getCardById_embedsFullFaqHistoryOrderedOldestToNewest() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("OP13-119")
                .cardName("Charlotte Katakuri")
                .build();
        var older = CardFaq.builder()
                .cardCode("OP13-119")
                .question("Does this trigger on Life cards?")
                .answer("No.")
                .publishedDate(LocalDate.of(2023, 6, 1))
                .build();
        var newer = CardFaq.builder()
                .cardCode("OP13-119")
                .question("Does this stack with other effects?")
                .answer("Yes.")
                .publishedDate(LocalDate.of(2024, 3, 3))
                .build();
        when(cardDetailsUseCase.getCardById("1"))
                .thenReturn(new CardDetails(card, List.of(), List.of(older, newer)));

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faq.length()").value(2))
                .andExpect(jsonPath("$.faq[0].question").value("Does this trigger on Life cards?"))
                .andExpect(jsonPath("$.faq[0].answer").value("No."))
                .andExpect(jsonPath("$.faq[1].question").value("Does this stack with other effects?"))
                .andExpect(jsonPath("$.faq[1].answer").value("Yes."));
    }

    @Test
    void getCardById_embedsThePriceForTheExactPrintedVariant() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("EB01-001")
                .cardName("Kouzuki Oden")
                .priceReference("single:EB01-001_p2")
                .build();
        when(cardDetailsUseCase.getCardById("1"))
                .thenReturn(new CardDetails(card, List.of(), List.of()));
        when(priceQueryUseCase.getLatestPricesByReferences(List.of("single:EB01-001_p2")))
                .thenReturn(Map.of("single:EB01-001_p2", List.of(priceQuote("single:EB01-001_p2"))));

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prices.length()").value(1))
                .andExpect(jsonPath("$.prices[0].source").value("CARDMARKET"))
                .andExpect(jsonPath("$.prices[0].trendPrice").value(45.19));
    }

    @Test
    void getCardVariants_embedsPriceForEveryExactPrintedVariant() throws Exception {
        var card = SetCard.builder()
                .id(2L)
                .cardSetId("EB01-001")
                .variantIndex("p1")
                .priceReference("single:EB01-001_p1")
                .build();
        var legacyCardWithoutPriceReference = SetCard.builder()
                .id(3L)
                .cardSetId("EB01-001")
                .variantIndex("r1")
                .build();
        when(cardDetailsUseCase.getCardVariants("1"))
                .thenReturn(List.of(
                        new CardDetails(card, List.of(), List.of()),
                        new CardDetails(legacyCardWithoutPriceReference, List.of(), List.of())));
        when(priceQueryUseCase.getLatestPricesByReferences(
                Arrays.asList("single:EB01-001_p1", null)))
                .thenReturn(Map.of("single:EB01-001_p1", List.of(priceQuote("single:EB01-001_p1"))));

        mockMvc.perform(get("/api/cards/1/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variantIndex").value("p1"))
                .andExpect(jsonPath("$[0].prices[0].productId").value("767954"))
                .andExpect(jsonPath("$[1].variantIndex").value("r1"))
                .andExpect(jsonPath("$[1].prices.length()").value(0));
    }

    @Test
    void getCardByCode_embedsTheChangeOnlyPriceHistoryOldestFirst() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("EB01-001")
                .cardName("Kouzuki Oden")
                .priceReference("single:EB01-001_p1")
                .build();
        when(cardDetailsUseCase.getCardByCode("EB01-001", "p1"))
                .thenReturn(new CardDetails(card, List.of(), List.of()));
        when(priceQueryUseCase.getPriceHistoryByReference("single:EB01-001_p1"))
                .thenReturn(List.of(
                        historyPoint("2026-08-13T04:00:00+02:00", "40.00", "35.00"),
                        historyPoint("2026-08-15T04:00:00+02:00", "45.19", "37.99")));

        mockMvc.perform(get("/api/cards/by-code")
                        .param("cardCode", "EB01-001")
                        .param("variant", "p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceHistory.length()").value(2))
                .andExpect(jsonPath("$.priceHistory[0].source").value("CARDMARKET"))
                .andExpect(jsonPath("$.priceHistory[0].currency").value("EUR"))
                .andExpect(jsonPath("$.priceHistory[0].observedAt").value("2026-08-13T04:00:00+02:00"))
                .andExpect(jsonPath("$.priceHistory[0].trendPrice").value(40.00))
                .andExpect(jsonPath("$.priceHistory[0].lowPrice").value(35.00))
                .andExpect(jsonPath("$.priceHistory[1].observedAt").value("2026-08-15T04:00:00+02:00"))
                .andExpect(jsonPath("$.priceHistory[1].trendPrice").value(45.19));
    }

    @Test
    void getCardById_embedsAnEmptyPriceHistoryForACardWithoutPriceReference() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("EB01-001")
                .cardName("Kouzuki Oden")
                .build();
        when(cardDetailsUseCase.getCardById("1"))
                .thenReturn(new CardDetails(card, List.of(), List.of()));

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceHistory.length()").value(0));
    }

    @Test
    void getCardVariants_omitsThePriceHistoryToAvoidAQueryPerVariant() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("EB01-001")
                .cardName("Kouzuki Oden")
                .priceReference("single:EB01-001_p1")
                .build();
        when(cardDetailsUseCase.getCardVariants("1"))
                .thenReturn(List.of(new CardDetails(card, List.of(), List.of())));

        mockMvc.perform(get("/api/cards/1/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priceHistory.length()").value(0));
        verify(priceQueryUseCase, never()).getPriceHistoryByReference(anyString());
    }

    private static PriceHistoryPoint historyPoint(String observedAt, String trendPrice, String lowPrice) {
        return PriceHistoryPoint.builder()
                .source(PriceSource.CARDMARKET)
                .currency("EUR")
                .observedAt(OffsetDateTime.parse(observedAt))
                .trendPrice(new BigDecimal(trendPrice))
                .lowPrice(new BigDecimal(lowPrice))
                .build();
    }
}
