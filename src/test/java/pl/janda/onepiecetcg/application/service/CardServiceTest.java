package pl.janda.onepiecetcg.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.application.model.CardSearchField;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private SetCardRepository setCardRepository;

    @Mock
    private CardFilterOptionService cardFilterOptionService;

    @Mock
    private SemanticQueryParser semanticQueryParser;

    private CardService cardService;

    @Test
    void searchCards_defaultsSearchFieldToNameWhenOmitted() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();

        // name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
        // attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page, limit, showAllVariants
        cardService.searchCards(
                "Luffy", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        var searchFieldCaptor = ArgumentCaptor.forClass(CardSearchField.class);
        verify(setCardRepository).search(
                any(), searchFieldCaptor.capture(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean());
        assertThat(searchFieldCaptor.getValue()).isEqualTo(CardSearchField.NAME);
    }

    @Test
    void searchCards_passesThroughExplicitSearchFieldUnchanged() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("DON")).thenReturn(new SemanticQueryParser.ParsedSemanticQuery("DON", null, null, null));

        cardService.searchCards(
                "DON", CardSearchField.SEMANTIC, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        var searchFieldCaptor = ArgumentCaptor.forClass(CardSearchField.class);
        verify(setCardRepository).search(
                any(), searchFieldCaptor.capture(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean());
        assertThat(searchFieldCaptor.getValue()).isEqualTo(CardSearchField.SEMANTIC);
    }

    @Test
    void searchCards_semanticMode_sidebarValueTakesPrecedenceOverParsedToken() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("rush 6c 2kc"))
                .thenReturn(new SemanticQueryParser.ParsedSemanticQuery("rush", 6, 2000, null));

        // Sidebar already sets power=7000; the parsed token has no power, so this doesn't matter here,
        // but counterAmount IS already set by the sidebar (5000) and must win over the parsed 2000.
        cardService.searchCards(
                "rush 6c 2kc", CardSearchField.SEMANTIC, null, null, null, null, List.of(3), 7000, 5000,
                null, null, null, null, null, null, null, null, null);

        var nameCaptor = ArgumentCaptor.forClass(String.class);
        var costsCaptor = ArgumentCaptor.forClass(List.class);
        var powerCaptor = ArgumentCaptor.forClass(Integer.class);
        var counterCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(setCardRepository).search(
                nameCaptor.capture(), any(), any(), any(), any(), any(), costsCaptor.capture(), powerCaptor.capture(), counterCaptor.capture(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean());

        assertThat(nameCaptor.getValue()).isEqualTo("rush");
        assertThat(costsCaptor.getValue()).containsExactlyInAnyOrder(3, 6);
        assertThat(powerCaptor.getValue()).isEqualTo(7000);
        assertThat(counterCaptor.getValue()).isEqualTo(5000);
    }

    @Test
    void searchCards_semanticMode_blankRemainingTextPassesEmptyNameToRepository() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("6c")).thenReturn(new SemanticQueryParser.ParsedSemanticQuery("", 6, null, null));

        cardService.searchCards(
                "6c", CardSearchField.SEMANTIC, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        var nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(setCardRepository).search(
                nameCaptor.capture(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean());
        assertThat(nameCaptor.getValue()).isEqualTo("");
    }

    private void stubRepository() {
        // search(): name, searchField, types, colors, rarities, flatRarities, costs, power,
        // counterAmount, attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page,
        // limit, showAllVariants
        when(setCardRepository.search(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean()))
                .thenReturn(List.of());
        // countSearch(): name, searchField, types, colors, rarities, flatRarities, costs, power,
        // counterAmount, attributes, attributeCombos, subTypes, prefixes
        when(setCardRepository.countSearch(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean()))
                .thenReturn(0L);
    }
}
