package pl.janda.onepiecetcg.pricing.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.model.PriceSource;
import pl.janda.onepiecetcg.pricing.application.repository.PriceQuoteRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceQueryServiceTest {

    @Mock
    private PriceQuoteRepository priceQuoteRepository;

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
}
