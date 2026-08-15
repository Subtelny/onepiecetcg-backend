package pl.janda.onepiecetcg.pricing.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.pricing.application.client.CardmarketPriceApiClient;
import pl.janda.onepiecetcg.pricing.application.client.CardmarketProductPageApiClient;
import pl.janda.onepiecetcg.pricing.application.client.PriceableSingleCatalogClient;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketExpansionRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketPriceCandidateRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketSingleMappingRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardmarketPriceSyncServiceTest {

    private static final OffsetDateTime GUIDE_CREATED_AT =
            OffsetDateTime.parse("2026-08-01T02:00:00+02:00");

    @Mock
    private CardmarketPriceApiClient cardmarketPriceApiClient;
    @Mock
    private CardmarketProductPageApiClient cardmarketProductPageApiClient;
    @Mock
    private PriceableSingleCatalogClient priceableSingleCatalogClient;
    @Mock
    private CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;
    @Mock
    private CardmarketExpansionRepository cardmarketExpansionRepository;
    @Mock
    private CardmarketSingleMappingRepository cardmarketSingleMappingRepository;
    @Mock
    private CardmarketExpansionMatcher cardmarketExpansionMatcher;
    @Mock
    private CardmarketSingleMatcher cardmarketSingleMatcher;
    @Mock
    private CardmarketPriceImportService cardmarketPriceImportService;

    private static CardmarketPriceCandidate candidate() {
        return CardmarketPriceCandidate.builder()
                .productId(101L)
                .cardCode("OP01-001")
                .expansionId(1001L)
                .productName("Luffy (OP01-001)")
                .priceGuideVersion("2")
                .priceGuideCreatedAt(GUIDE_CREATED_AT)
                .build();
    }

    @Test
    void syncPrices_appendsANewGuideAndStampsCandidates() {
        var candidate = candidate();
        var expansion = CardmarketExpansion.builder()
                .expansionId(1001L)
                .releaseId("569201")
                .lastResolvedAt(LocalDateTime.now())
                .build();
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of(candidate));
        when(cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(GUIDE_CREATED_AT)).thenReturn(false);
        when(priceableSingleCatalogClient.fetchPriceableSingles()).thenReturn(List.of());
        when(cardmarketSingleMappingRepository.findAll()).thenReturn(List.of());
        when(cardmarketExpansionRepository.findAll()).thenReturn(List.of(expansion));
        when(cardmarketProductPageApiClient.resolveProductPages(any())).thenReturn(List.of());
        when(cardmarketSingleMatcher.findVersionResolutionRequests(any(), any(), any(), any())).thenReturn(List.of());
        when(cardmarketSingleMatcher.match(any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        service().syncPrices();

        verify(cardmarketPriceImportService).append(List.of(expansion), List.of(), List.of(candidate));
        assertThat(candidate.getLastSyncedAt()).isNotNull();
    }

    @Test
    void syncPrices_keepsExistingHistoryWhenNoCandidatesWereMapped() {
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of());

        assertThatThrownBy(() -> service().syncPrices())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("keeping the existing snapshot");
        verify(cardmarketPriceImportService, never()).append(any(), any(), any());
    }

    @Test
    void syncPrices_skipsAnAlreadyStoredGuideBeforeEnrichment() {
        var candidate = candidate();
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of(candidate));
        when(cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(GUIDE_CREATED_AT)).thenReturn(true);

        service().syncPrices();

        verify(priceableSingleCatalogClient, never()).fetchPriceableSingles();
        verify(cardmarketProductPageApiClient, never()).resolveProductPages(any());
        verify(cardmarketPriceImportService, never()).append(any(), any(), any());
        assertThat(candidate.getLastSyncedAt()).isNull();
    }

    private CardmarketPriceSyncService service() {
        return new CardmarketPriceSyncService(
                cardmarketPriceApiClient,
                cardmarketProductPageApiClient,
                priceableSingleCatalogClient,
                cardmarketPriceCandidateRepository,
                cardmarketExpansionRepository,
                cardmarketSingleMappingRepository,
                cardmarketExpansionMatcher,
                cardmarketSingleMatcher,
                cardmarketPriceImportService);
    }
}
