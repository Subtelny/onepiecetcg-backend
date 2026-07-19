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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private SetCardRepository setCardRepository;

    @Mock
    private CardFilterOptionService cardFilterOptionService;

    private CardService cardService;

    @Test
    void searchCards_defaultsSearchFieldToNameWhenOmitted() {
        cardService = new CardService(setCardRepository, cardFilterOptionService);
        stubRepository();

        // name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
        // attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page, limit
        cardService.searchCards(
                "Luffy", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);

        var searchFieldCaptor = ArgumentCaptor.forClass(CardSearchField.class);
        verify(setCardRepository).search(
                any(), searchFieldCaptor.capture(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
        assertThat(searchFieldCaptor.getValue()).isEqualTo(CardSearchField.NAME);
    }

    @Test
    void searchCards_passesThroughExplicitSearchFieldUnchanged() {
        cardService = new CardService(setCardRepository, cardFilterOptionService);
        stubRepository();

        cardService.searchCards(
                "DON", CardSearchField.DESCRIPTION, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);

        var searchFieldCaptor = ArgumentCaptor.forClass(CardSearchField.class);
        verify(setCardRepository).search(
                any(), searchFieldCaptor.capture(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt());
        assertThat(searchFieldCaptor.getValue()).isEqualTo(CardSearchField.DESCRIPTION);
    }

    private void stubRepository() {
        // search(): name, searchField, types, colors, rarities, flatRarities, costs, power,
        // counterAmount, attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page, limit
        when(setCardRepository.search(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        // countSearch(): name, searchField, types, colors, rarities, flatRarities, costs, power,
        // counterAmount, attributes, attributeCombos, subTypes, prefixes
        when(setCardRepository.countSearch(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any()))
                .thenReturn(0L);
    }
}
