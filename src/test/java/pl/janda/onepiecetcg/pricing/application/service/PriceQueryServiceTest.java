package pl.janda.onepiecetcg.pricing.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.pricing.application.model.PriceHistoryPoint;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.model.PriceSource;
import pl.janda.onepiecetcg.pricing.application.repository.PriceHistoryRepository;
import pl.janda.onepiecetcg.pricing.application.repository.PriceQuoteRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceQueryServiceTest {

    @Mock
    private PriceQuoteRepository priceQuoteRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks
    private PriceQueryService priceQueryService;

    private static PriceQuote quote(String priceReference, String productId) {
        return PriceQuote.builder()
                .priceReference(priceReference)
                .source(PriceSource.CARDMARKET)
                .externalProductId(productId)
                .build();
    }

    @Test
    void getLatestPricesByReferences_filtersInvalidReferencesAndGroupsQuotesByReference() {
        var first = quote("single:EB01-001", "767953");
        var second = quote("single:EB01-001_p1", "767954");
        when(priceQuoteRepository.findLatestByPriceReferences(
                List.of("single:EB01-001", "single:EB01-001_p1")))
                .thenReturn(List.of(first, second));

        var result = priceQueryService.getLatestPricesByReferences(Arrays.asList(
                "single:EB01-001",
                null,
                " ",
                "single:EB01-001",
                "single:EB01-001_p1"));

        assertThat(result)
                .containsEntry("single:EB01-001", List.of(first))
                .containsEntry("single:EB01-001_p1", List.of(second));
        verify(priceQuoteRepository).findLatestByPriceReferences(
                List.of("single:EB01-001", "single:EB01-001_p1"));
    }

    @Test
    void getLatestPricesByReferences_doesNotQueryRepositoryForEmptyInput() {
        assertThat(priceQueryService.getLatestPricesByReferences(List.of())).isEmpty();
        assertThat(priceQueryService.getLatestPricesByReferences(null)).isEmpty();

        verify(priceQuoteRepository, never()).findLatestByPriceReferences(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getPriceHistoryByReference_delegatesToTheHistoryRepository() {
        var point = PriceHistoryPoint.builder()
                .source(PriceSource.CARDMARKET)
                .currency("EUR")
                .observedAt(OffsetDateTime.parse("2026-08-15T04:00:00+02:00"))
                .trendPrice(new BigDecimal("45.19"))
                .lowPrice(new BigDecimal("37.99"))
                .build();
        when(priceHistoryRepository.findHistoryByPriceReference("single:EB01-001"))
                .thenReturn(List.of(point));

        assertThat(priceQueryService.getPriceHistoryByReference("single:EB01-001"))
                .containsExactly(point);
    }

    @Test
    void getPriceHistoryByReference_doesNotQueryRepositoryForMissingReference() {
        assertThat(priceQueryService.getPriceHistoryByReference(null)).isEmpty();
        assertThat(priceQueryService.getPriceHistoryByReference(" ")).isEmpty();

        verify(priceHistoryRepository, never()).findHistoryByPriceReference(anyString());
    }
}
