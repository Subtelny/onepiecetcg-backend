package pl.janda.onepiecetcg.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.janda.onepiecetcg.application.OnePieceTcgApplication;
import pl.janda.onepiecetcg.application.model.CardErrata;
import pl.janda.onepiecetcg.application.model.CardSearchField;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.service.CardErrataService;
import pl.janda.onepiecetcg.application.service.CardService;
import pl.janda.onepiecetcg.application.service.PagedCards;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// OnePieceTcgApplication lives under application/, not the root package, so @SpringBootTest's
// upward package scan can't find it - declared explicitly here.
@SpringBootTest(classes = OnePieceTcgApplication.class)
@AutoConfigureMockMvc
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    @MockitoBean
    private CardErrataService cardErrataService;

    // CardService.searchCards has 17 params, in this order:
    // name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
    // attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page, limit

    @Test
    void searchCards_deserializesSearchInEnumAndForwardsToService() throws Exception {
        when(cardService.searchCards(
                any(), // name
                any(), // searchField
                any(), // types
                any(), // colors
                any(), // rarities
                any(), // flatRarities
                any(), // costs
                any(), // power
                any(), // counterAmount
                any(), // attributes
                any(), // attributeCombos
                any(), // subTypes
                any(), // prefixes
                any(), // sortBy
                any(), // sortOrder
                any(), // page
                any(), // limit
                any()  // showAllVariants
        )).thenReturn(new PagedCards(List.of(), 0, 0, 50));

        mockMvc.perform(get("/api/cards").param("name", "foo").param("searchIn", "SEMANTIC"))
                .andExpect(status().isOk());

        verify(cardService).searchCards(
                eq("foo"),
                eq(CardSearchField.SEMANTIC),
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchCards_omittingSearchInLeavesItNullForServiceToDefault() throws Exception {
        when(cardService.searchCards(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PagedCards(List.of(), 0, 0, 50));

        mockMvc.perform(get("/api/cards").param("name", "Luffy"))
                .andExpect(status().isOk());

        verify(cardService).searchCards(
                eq("Luffy"),
                eq(null),
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any());
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
        when(cardErrataService.listAll()).thenReturn(List.of(errata));

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
        when(cardService.getCardById("1")).thenReturn(card);

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
        when(cardErrataService.historyByCardCodes(List.of("OP13-119")))
                .thenReturn(Map.of("OP13-119", List.of(older, newer)));

        mockMvc.perform(get("/api/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errata.length()").value(2))
                .andExpect(jsonPath("$.errata[0].date").value("2023-06-01"))
                .andExpect(jsonPath("$.errata[0].after").value("Middle text"))
                .andExpect(jsonPath("$.errata[0].note").value("Also applies to parallel card version."))
                .andExpect(jsonPath("$.errata[1].date").value("2024-03-03"))
                .andExpect(jsonPath("$.errata[1].after").value("New text"));
    }
}
