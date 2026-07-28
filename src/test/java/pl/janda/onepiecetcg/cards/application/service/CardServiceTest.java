package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.CardSearchField;
import pl.janda.onepiecetcg.cards.application.repository.SetCardRepository;

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
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean());
        assertThat(searchFieldCaptor.getValue()).isEqualTo(CardSearchField.NAME);
    }

    @Test
    void searchCards_passesThroughExplicitSearchFieldUnchanged() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("DON")).thenReturn(new SemanticQueryParser.ParsedSemanticQuery("DON", null, null, null, false));

        cardService.searchCards(
                "DON", CardSearchField.SEMANTIC, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        var searchFieldCaptor = ArgumentCaptor.forClass(CardSearchField.class);
        verify(setCardRepository).search(
                any(), searchFieldCaptor.capture(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean());
        assertThat(searchFieldCaptor.getValue()).isEqualTo(CardSearchField.SEMANTIC);
    }

    @Test
    void searchCards_semanticMode_sidebarValueTakesPrecedenceOverParsedToken() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("rush 6c 2kc"))
                .thenReturn(new SemanticQueryParser.ParsedSemanticQuery("rush", 6, 2000, null, false));

        // Sidebar already sets costs=[3] and counterAmount=5000; both must win over the parsed
        // tokens (6 and 2000 respectively). power has no sidebar value, so it's left untouched here.
        cardService.searchCards(
                "rush 6c 2kc", CardSearchField.SEMANTIC, null, null, null, null, List.of(3), null, 5000,
                null, null, null, null, null, null, null, null, null);

        var nameCaptor = ArgumentCaptor.forClass(String.class);
        var costsCaptor = ArgumentCaptor.forClass(List.class);
        var powerCaptor = ArgumentCaptor.forClass(Integer.class);
        var counterCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(setCardRepository).search(
                nameCaptor.capture(), any(), any(), any(), any(), any(), costsCaptor.capture(), powerCaptor.capture(), counterCaptor.capture(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean());

        assertThat(nameCaptor.getValue()).isEqualTo("rush");
        assertThat(costsCaptor.getValue()).containsExactly(3);
        assertThat(powerCaptor.getValue()).isNull();
        assertThat(counterCaptor.getValue()).isEqualTo(5000);
    }

    @Test
    void searchCards_semanticMode_parsedCostTokenUsedWhenSidebarCostsAbsent() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("rush 6c"))
                .thenReturn(new SemanticQueryParser.ParsedSemanticQuery("rush", 6, null, null, false));

        // No sidebar costs filter provided, so the parsed token (6) is used as-is.
        cardService.searchCards(
                "rush 6c", CardSearchField.SEMANTIC, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        var costsCaptor = ArgumentCaptor.forClass(List.class);
        verify(setCardRepository).search(
                any(), any(), any(), any(), any(), any(), costsCaptor.capture(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean());

        assertThat(costsCaptor.getValue()).containsExactly(6);
    }

    @Test
    void searchCards_semanticMode_blankRemainingTextPassesEmptyNameToRepository() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("6c")).thenReturn(new SemanticQueryParser.ParsedSemanticQuery("", 6, null, null, false));

        cardService.searchCards(
                "6c", CardSearchField.SEMANTIC, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        var nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(setCardRepository).search(
                nameCaptor.capture(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean());
        assertThat(nameCaptor.getValue()).isEqualTo("");
    }

    @Test
    void searchCards_semanticMode_errataKeyword_forwardsErrataOnlyTrueToRepository() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("errata Luffy"))
                .thenReturn(new SemanticQueryParser.ParsedSemanticQuery("Luffy", null, null, null, true));

        cardService.searchCards(
                "errata Luffy", CardSearchField.SEMANTIC, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        var errataOnlyCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(setCardRepository).search(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean(), errataOnlyCaptor.capture());
        assertThat(errataOnlyCaptor.getValue()).isTrue();
    }

    @Test
    void searchCards_nameMode_neverSetsErrataOnly() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();

        cardService.searchCards(
                "Luffy", CardSearchField.NAME, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        var errataOnlyCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(setCardRepository).search(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean(), errataOnlyCaptor.capture());
        assertThat(errataOnlyCaptor.getValue()).isFalse();
    }

    private void stubRepository() {
        // search(): name, searchField, types, colors, rarities, flatRarities, costs, power,
        // counterAmount, attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page,
        // limit, showAllVariants
        when(setCardRepository.search(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of());
        // countSearch(): name, searchField, types, colors, rarities, flatRarities, costs, power,
        // counterAmount, attributes, attributeCombos, subTypes, prefixes
        when(setCardRepository.countSearch(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(0L);
    }
}
