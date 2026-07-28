package pl.janda.onepiecetcg.deckbuilder.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.janda.onepiecetcg.application.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.cards.application.model.CardSearchField;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.service.CardService;
import pl.janda.onepiecetcg.cards.application.service.PagedCards;

import java.util.List;

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
class DeckBuilderCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardService cardService;

    // CardService.searchCards has 18 params, in this order:
    // name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
    // attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page, limit, showAllVariants

    @Test
    void searchCards_deserializesSearchInEnumAndForwardsToSharedCardService() throws Exception {
        when(cardService.searchCards(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(new PagedCards(List.of(), 0, 0, 50));

        mockMvc.perform(get("/api/deckbuilder/cards").param("name", "foo").param("searchIn", "SEMANTIC"))
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

        mockMvc.perform(get("/api/deckbuilder/cards").param("name", "Luffy"))
                .andExpect(status().isOk());

        verify(cardService).searchCards(
                eq("Luffy"),
                eq(null),
                any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getFilterOptions_returnsFilterOptionsFromSharedCardService() throws Exception {
        var options = CardFilterOptions.builder()
                .types(List.of("LEADER"))
                .colors(List.of("RED"))
                .rarities(List.of())
                .flatRarities(List.of())
                .costs(List.of())
                .sets(List.of())
                .attributes(List.of())
                .attributeCombos(List.of())
                .subTypes(List.of())
                .prefixes(List.of())
                .build();
        when(cardService.getFilterOptions()).thenReturn(options);

        mockMvc.perform(get("/api/deckbuilder/cards/filters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types[0]").value("LEADER"))
                .andExpect(jsonPath("$.colors[0]").value("RED"));
    }

    @Test
    void getCardByCode_returnsMappedCardWithoutErrataOrFaq() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("OP10-009")
                .cardName("Monkey.D.Luffy")
                .build();
        when(cardService.getVariantByCardCode("OP10-009", null)).thenReturn(card);

        mockMvc.perform(get("/api/deckbuilder/cards/by-code").param("cardCode", "OP10-009"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.cardNumber").value("OP10-009"))
                .andExpect(jsonPath("$.name").value("Monkey.D.Luffy"));
    }

    @Test
    void getCardById_returnsMappedCardWithoutErrataOrFaq() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("OP13-119")
                .cardName("Charlotte Katakuri")
                .build();
        when(cardService.getCardById("1")).thenReturn(card);

        mockMvc.perform(get("/api/deckbuilder/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Charlotte Katakuri"));
    }

    @Test
    void getCardVariants_returnsMappedVariantList() throws Exception {
        var variant = SetCard.builder()
                .id(2L)
                .cardSetId("OP13-119_p1")
                .cardName("Charlotte Katakuri")
                .build();
        when(cardService.getVariantsByCardId("1")).thenReturn(List.of(variant));

        mockMvc.perform(get("/api/deckbuilder/cards/1/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cardNumber").value("OP13-119_p1"));
    }
}
