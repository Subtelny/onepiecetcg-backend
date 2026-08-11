package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.CardSearchCriteria;
import pl.janda.onepiecetcg.cards.application.model.CardSearchField;
import pl.janda.onepiecetcg.cards.application.model.CardSearchQuery;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.repository.SetCardQueryRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private SetCardQueryRepository setCardRepository;

    @Mock
    private CardFilterOptionService cardFilterOptionService;

    @Mock
    private SemanticQueryParser semanticQueryParser;

    private CardService cardService;

    private static CardSearchQuery query(
            String text,
            CardSearchField searchField,
            List<Integer> costs,
            Integer power,
            Integer counterAmount
    ) {
        return new CardSearchQuery(
                text, searchField, null, null, null, null, costs, power, counterAmount,
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    void searchCards_defaultsSearchFieldToNameWhenOmitted() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();

        cardService.searchCards(query("Luffy", null, null, null, null));

        assertThat(capturedCriteria().searchField()).isEqualTo(CardSearchField.NAME);
    }

    @Test
    void searchCards_passesThroughExplicitSearchFieldUnchanged() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("DON")).thenReturn(new SemanticQueryParser.ParsedSemanticQuery("DON", null, null, null, false));

        cardService.searchCards(query("DON", CardSearchField.SEMANTIC, null, null, null));

        assertThat(capturedCriteria().searchField()).isEqualTo(CardSearchField.SEMANTIC);
    }

    @Test
    void searchCards_semanticMode_sidebarValueTakesPrecedenceOverParsedToken() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("rush 6c 2kc"))
                .thenReturn(new SemanticQueryParser.ParsedSemanticQuery("rush", 6, 2000, null, false));


        cardService.searchCards(query("rush 6c 2kc", CardSearchField.SEMANTIC, List.of(3), null, 5000));

        var criteria = capturedCriteria();
        assertThat(criteria.text()).isEqualTo("rush");
        assertThat(criteria.costs()).containsExactly(3);
        assertThat(criteria.power()).isNull();
        assertThat(criteria.counterAmount()).isEqualTo(5000);
    }

    @Test
    void searchCards_semanticMode_parsedCostTokenUsedWhenSidebarCostsAbsent() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("rush 6c"))
                .thenReturn(new SemanticQueryParser.ParsedSemanticQuery("rush", 6, null, null, false));


        cardService.searchCards(query("rush 6c", CardSearchField.SEMANTIC, null, null, null));

        assertThat(capturedCriteria().costs()).containsExactly(6);
    }

    @Test
    void searchCards_semanticMode_blankRemainingTextPassesEmptyNameToRepository() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("6c")).thenReturn(new SemanticQueryParser.ParsedSemanticQuery("", 6, null, null, false));

        cardService.searchCards(query("6c", CardSearchField.SEMANTIC, null, null, null));

        assertThat(capturedCriteria().text()).isEqualTo("");
    }

    @Test
    void searchCards_semanticMode_errataKeyword_forwardsErrataOnlyTrueToRepository() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();
        when(semanticQueryParser.parse("errata Luffy"))
                .thenReturn(new SemanticQueryParser.ParsedSemanticQuery("Luffy", null, null, null, true));

        cardService.searchCards(query("errata Luffy", CardSearchField.SEMANTIC, null, null, null));

        assertThat(capturedCriteria().errataOnly()).isTrue();
    }

    @Test
    void searchCards_nameMode_neverSetsErrataOnly() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        stubRepository();

        cardService.searchCards(query("Luffy", CardSearchField.NAME, null, null, null));

        assertThat(capturedCriteria().errataOnly()).isFalse();
    }

    @Test
    void getRepresentativeCardsByCardCodes_usesSingleBulkRepositoryLookup() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        var cards = List.of(SetCard.builder().cardSetId("OP01-001").variantIndex("0").build());
        when(setCardRepository.findRepresentativesByCardSetIds(List.of("OP01-001", "OP01-006")))
                .thenReturn(cards);

        var result = cardService.getRepresentativeCardsByCardCodes(List.of("OP01-001", "OP01-006"));

        assertThat(result).isSameAs(cards);
        verify(setCardRepository).findRepresentativesByCardSetIds(List.of("OP01-001", "OP01-006"));
    }

    @Test
    void getVariantsByCardId_ordersByPersistedVariantIndex() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        var selected = SetCard.builder().id(12L).cardSetId("OP01-001").variantIndex("r1").build();
        when(setCardRepository.findById(12L)).thenReturn(java.util.Optional.of(selected));
        when(setCardRepository.findByCardSetId("OP01-001")).thenReturn(List.of(
                selected,
                SetCard.builder().id(10L).cardSetId("OP01-001").variantIndex("p10").build(),
                SetCard.builder().id(11L).cardSetId("OP01-001").variantIndex("0").build(),
                SetCard.builder().id(13L).cardSetId("OP01-001").variantIndex("p2").build(),
                SetCard.builder().id(14L).cardSetId("OP01-001").variantIndex("p1").build()
        ));

        var result = cardService.getVariantsByCardId("12");

        assertThat(result).extracting(SetCard::getVariantIndex).containsExactly("0", "p1", "p2", "p10", "r1");
    }

    @Test
    void getVariantByCardCode_usesExactSourceDerivedVariantIndex() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);
        var reprint = SetCard.builder().cardSetId("OP01-001").variantIndex("r1").build();
        when(setCardRepository.findByCardSetIdAndVariantIndex("OP01-001", "r1"))
                .thenReturn(java.util.Optional.of(reprint));

        var result = cardService.getVariantByCardCode("OP01-001", "R1");

        assertThat(result).isSameAs(reprint);
        verify(setCardRepository).findByCardSetIdAndVariantIndex("OP01-001", "r1");
    }

    @Test
    void getVariantByCardCode_rejectsUnsupportedVariantIndex() {
        cardService = new CardService(setCardRepository, cardFilterOptionService, semanticQueryParser);

        assertThatThrownBy(() -> cardService.getVariantByCardCode("OP01-001", "1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid variant index");
    }

    private void stubRepository() {
        when(setCardRepository.search(any(CardSearchCriteria.class))).thenReturn(List.of());
        when(setCardRepository.countSearch(any(CardSearchCriteria.class))).thenReturn(0L);
    }

    private CardSearchCriteria capturedCriteria() {
        var captor = ArgumentCaptor.forClass(CardSearchCriteria.class);
        verify(setCardRepository).search(captor.capture());
        return captor.getValue();
    }
}
