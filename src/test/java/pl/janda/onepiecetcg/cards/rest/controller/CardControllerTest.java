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
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private CardSetSyncUseCase cardSetSyncUseCase;

    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;

    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;

    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;

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
                .variantIndex("r1")
                .build();
        when(cardCatalogUseCase.searchCards(any(CardSearchQuery.class)))
                .thenReturn(new PagedCards(List.of(summary), 1, 0, 50));

        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[0].name").value("Nami"))
                .andExpect(jsonPath("$.cards[0].displayName").value("Nami (Winner)"))
                .andExpect(jsonPath("$.cards[0].sourceProduct").value("Winner Pack 2026 Vol. 2"))
                .andExpect(jsonPath("$.cards[0].variantIndex").value("r1"));
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
                .andExpect(jsonPath("$.errata[1].after").value("New text"));
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
}
