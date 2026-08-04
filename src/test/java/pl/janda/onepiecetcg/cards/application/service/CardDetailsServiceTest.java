package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.CardErrata;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataQueryUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqQueryUseCase;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardDetailsServiceTest {

    @Mock
    private CardCatalogUseCase cardCatalogUseCase;

    @Mock
    private CardErrataQueryUseCase cardErrataQueryUseCase;

    @Mock
    private CardFaqQueryUseCase cardFaqQueryUseCase;

    @Test
    void getCardVariants_resolvesRulesHistoryInBatchesAndBuildsAggregates() {
        var first = SetCard.builder().id(1L).cardSetId("OP01-001").build();
        var second = SetCard.builder().id(2L).cardSetId("OP01-002").build();
        var errata = CardErrata.builder().cardCode("OP01-001").build();
        var faq = CardFaq.builder().cardCode("OP01-002").build();
        var service = new CardDetailsService(cardCatalogUseCase, cardErrataQueryUseCase, cardFaqQueryUseCase);

        when(cardCatalogUseCase.getVariantsByCardId("1")).thenReturn(List.of(first, second));
        when(cardErrataQueryUseCase.historyByCardCodes(List.of("OP01-001", "OP01-002")))
                .thenReturn(Map.of("OP01-001", List.of(errata)));
        when(cardFaqQueryUseCase.historyByCardCodes(List.of("OP01-001", "OP01-002")))
                .thenReturn(Map.of("OP01-002", List.of(faq)));

        var details = service.getCardVariants("1");

        assertThat(details).hasSize(2);
        assertThat(details.get(0).errata()).containsExactly(errata);
        assertThat(details.get(0).faq()).isEmpty();
        assertThat(details.get(1).errata()).isEmpty();
        assertThat(details.get(1).faq()).containsExactly(faq);
        verify(cardErrataQueryUseCase).historyByCardCodes(List.of("OP01-001", "OP01-002"));
        verify(cardFaqQueryUseCase).historyByCardCodes(List.of("OP01-001", "OP01-002"));
    }
}
