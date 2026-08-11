package pl.janda.onepiecetcg.deckbuilder.rest.controller;

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
class DeckBuilderCardControllerTest extends PostgresSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardCatalogUseCase cardCatalogUseCase;

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
    void searchCards_deserializesSearchInEnumAndForwardsToSharedCardService() throws Exception {
        when(cardCatalogUseCase.searchCards(any(CardSearchQuery.class)))
                .thenReturn(new PagedCards(List.of(), 0, 0, 50));

        mockMvc.perform(get("/api/deckbuilder/cards").param("name", "foo").param("searchIn", "SEMANTIC"))
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

        mockMvc.perform(get("/api/deckbuilder/cards").param("name", "Luffy"))
                .andExpect(status().isOk());

        var captor = org.mockito.ArgumentCaptor.forClass(CardSearchQuery.class);
        verify(cardCatalogUseCase).searchCards(captor.capture());
        assertThat(captor.getValue().text()).isEqualTo("Luffy");
        assertThat(captor.getValue().searchField()).isNull();
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
        when(cardCatalogUseCase.getFilterOptions()).thenReturn(options);

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
        when(cardCatalogUseCase.getVariantByCardCode("OP10-009", null)).thenReturn(card);

        mockMvc.perform(get("/api/deckbuilder/cards/by-code").param("cardCode", "OP10-009"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.cardNumber").value("OP10-009"))
                .andExpect(jsonPath("$.name").value("Monkey.D.Luffy"));
    }

    @Test
    void getCardByCode_forwardsTextVariantIndex() throws Exception {
        var card = SetCard.builder()
                .id(2L)
                .cardSetId("OP16-079")
                .variantIndex("p1")
                .build();
        when(cardCatalogUseCase.getVariantByCardCode("OP16-079", "p1")).thenReturn(card);

        mockMvc.perform(get("/api/deckbuilder/cards/by-code")
                        .param("cardCode", "OP16-079")
                        .param("variant", "p1"))
                .andExpect(status().isOk());

        verify(cardCatalogUseCase).getVariantByCardCode("OP16-079", "p1");
    }

    @Test
    void getCardById_returnsMappedCardWithoutErrataOrFaq() throws Exception {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("OP13-119")
                .cardName("Charlotte Katakuri")
                .build();
        when(cardCatalogUseCase.getCardById("1")).thenReturn(card);

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
        when(cardCatalogUseCase.getVariantsByCardId("1")).thenReturn(List.of(variant));

        mockMvc.perform(get("/api/deckbuilder/cards/1/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cardNumber").value("OP13-119_p1"));
    }
}
