package pl.janda.onepiecetcg.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.janda.onepiecetcg.application.OnePieceTcgApplication;
import pl.janda.onepiecetcg.application.model.CardSearchField;
import pl.janda.onepiecetcg.application.service.CardService;
import pl.janda.onepiecetcg.application.service.PagedCards;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
