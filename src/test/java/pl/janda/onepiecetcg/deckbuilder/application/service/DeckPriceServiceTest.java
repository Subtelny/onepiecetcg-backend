package pl.janda.onepiecetcg.deckbuilder.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.CardVariantReference;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceItem;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.model.PriceSource;
import pl.janda.onepiecetcg.pricing.application.port.in.PriceQueryUseCase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckPriceServiceTest {

    @Mock
    private CardCatalogUseCase cardCatalogUseCase;

    @Mock
    private PriceQueryUseCase priceQueryUseCase;

    private static SetCard card(String cardCode, String variantIndex, String priceReference) {
        return SetCard.builder()
                .cardSetId(cardCode)
                .variantIndex(variantIndex)
                .priceReference(priceReference)
                .build();
    }

    private static PriceQuote quote(String currency, String trendPrice, String averagePrice) {
        return PriceQuote.builder()
                .source(PriceSource.CARDMARKET)
                .currency(currency)
                .trendPrice(trendPrice != null ? new BigDecimal(trendPrice) : null)
                .averagePrice(averagePrice != null ? new BigDecimal(averagePrice) : null)
                .lowPrice(new BigDecimal("1.00"))
                .build();
    }

    @Test
    void calculateDeckPrice_batchesLookupsAndAggregatesCurrentPricesByCurrency() {
        var service = new DeckPriceService(cardCatalogUseCase, priceQueryUseCase);
        var defaultReference = new CardVariantReference("OP01-001", "0");
        var parallelReference = new CardVariantReference("OP02-002", "p1");
        var missingReference = new CardVariantReference("OP03-003", "0");
        var defaultCard = card("OP01-001", "0", "single:OP01-001");
        var parallelCard = card("OP02-002", "p1", "single:OP02-002_p1");

        when(cardCatalogUseCase.getCardsByVariantReferences(
                List.of(defaultReference, parallelReference, missingReference)))
                .thenReturn(List.of(defaultCard, parallelCard));
        when(priceQueryUseCase.getLatestPricesByReferences(
                List.of("single:OP01-001", "single:OP02-002_p1")))
                .thenReturn(Map.of(
                        "single:OP01-001", List.of(quote("EUR", "2.50", "2.10")),
                        "single:OP02-002_p1", List.of(quote("EUR", null, "3.00"))));

        var summary = service.calculateDeckPrice(List.of(
                new DeckPriceItem("OP01-001", "0", 2),
                new DeckPriceItem("OP01-001", "0", 1),
                new DeckPriceItem("OP02-002", "P1", 4),
                new DeckPriceItem("OP03-003", "0", 1)));

        assertThat(summary.totalCopies()).isEqualTo(8);
        assertThat(summary.pricedCopies()).isEqualTo(7);
        assertThat(summary.totals()).singleElement().satisfies(total -> {
            assertThat(total.currency()).isEqualTo("EUR");
            assertThat(total.amount()).isEqualByComparingTo("19.50");
        });
        verify(cardCatalogUseCase).getCardsByVariantReferences(
                List.of(defaultReference, parallelReference, missingReference));
        verify(priceQueryUseCase).getLatestPricesByReferences(
                List.of("single:OP01-001", "single:OP02-002_p1"));
    }

    @Test
    void calculateDeckPrice_doesNotCountAQuoteWithOnlyALowPrice() {
        var service = new DeckPriceService(cardCatalogUseCase, priceQueryUseCase);
        var reference = new CardVariantReference("OP01-001", "0");
        var card = card("OP01-001", "0", "single:OP01-001");
        when(cardCatalogUseCase.getCardsByVariantReferences(List.of(reference))).thenReturn(List.of(card));
        when(priceQueryUseCase.getLatestPricesByReferences(List.of("single:OP01-001")))
                .thenReturn(Map.of("single:OP01-001", List.of(quote("EUR", null, null))));

        var summary = service.calculateDeckPrice(List.of(new DeckPriceItem("OP01-001", "0", 4)));

        assertThat(summary.totalCopies()).isEqualTo(4);
        assertThat(summary.pricedCopies()).isZero();
        assertThat(summary.totals()).isEmpty();
    }

    @Test
    void calculateDeckPrice_returnsEmptySummaryWithoutCallingDependencies() {
        var service = new DeckPriceService(cardCatalogUseCase, priceQueryUseCase);

        assertThat(service.calculateDeckPrice(List.of()))
                .isEqualTo(new pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceSummary(
                        List.of(), 0, 0));
        verify(cardCatalogUseCase, never()).getCardsByVariantReferences(org.mockito.ArgumentMatchers.anyList());
        verify(priceQueryUseCase, never()).getLatestPricesByReferences(org.mockito.ArgumentMatchers.anyList());
    }
}
