package pl.janda.onepiecetcg.pricing.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.pricing.application.client.CardmarketPriceApiClient;
import pl.janda.onepiecetcg.pricing.application.client.PriceableSingleCatalogClient;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.pricing.application.model.PriceableSingle;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketExpansionRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketPriceCandidateRepository;

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
    private PriceableSingleCatalogClient priceableSingleCatalogClient;
    @Mock
    private CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;
    @Mock
    private CardmarketExpansionRepository cardmarketExpansionRepository;
    @Mock
    private CardmarketExpansionMatcher cardmarketExpansionMatcher;
    @Mock
    private CardmarketSingleMatcher cardmarketSingleMatcher;
    @Mock
    private CardmarketPriceImportService cardmarketPriceImportService;

    private static CardmarketPriceCandidate candidate(Long productId, Long expansionId) {
        return CardmarketPriceCandidate.builder()
                .productId(productId)
                .cardCode("OP01-001")
                .expansionId(expansionId)
                .productName("Luffy (OP01-001)")
                .priceGuideVersion("2")
                .priceGuideCreatedAt(GUIDE_CREATED_AT)
                .build();
    }

    private static PriceableSingle single() {
        return PriceableSingle.builder()
                .priceReference("single:OP01-001")
                .sourceCardId("OP01-001")
                .cardCode("OP01-001")
                .releaseId("569201")
                .variantIndex("0")
                .build();
    }

    @Test
    void syncPrices_appendsANewGuideAndStampsCandidates() {
        var candidate = candidate(101L, 1001L);
        var expansion = CardmarketExpansion.builder()
                .expansionId(1001L)
                .releaseId("569201")
                .lastResolvedAt(LocalDateTime.now())
                .build();
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of(candidate));
        when(cardmarketPriceApiClient.fetchNonEnglishExpansionIds()).thenReturn(List.of());
        when(cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(GUIDE_CREATED_AT)).thenReturn(false);
        when(priceableSingleCatalogClient.fetchPriceableSingles()).thenReturn(List.of(single()));
        when(cardmarketExpansionRepository.findAll()).thenReturn(List.of(expansion));
        when(cardmarketExpansionMatcher.match(any(), any(), any(), any(), any())).thenReturn(List.of(expansion));
        when(cardmarketSingleMatcher.match(any(), any(), any(), any())).thenReturn(List.of());

        service().syncPrices();

        verify(cardmarketPriceImportService).importPricingSnapshot(List.of(expansion), List.of(), List.of(candidate));
        assertThat(candidate.getLastSyncedAt()).isNotNull();
    }

    @Test
    void syncPrices_keepsTheJapanesePrintRunOutOfMatchingAndHistory() {
        var english = candidate(101L, 1001L);
        var japanese = candidate(202L, 2002L);
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of(english, japanese));
        when(cardmarketPriceApiClient.fetchNonEnglishExpansionIds()).thenReturn(List.of(2002L));
        when(cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(GUIDE_CREATED_AT)).thenReturn(false);
        when(priceableSingleCatalogClient.fetchPriceableSingles()).thenReturn(List.of(single()));
        when(cardmarketExpansionRepository.findAll()).thenReturn(List.of());
        when(cardmarketExpansionMatcher.match(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(cardmarketSingleMatcher.match(any(), any(), any(), any())).thenReturn(List.of());

        service().syncPrices();

        ArgumentCaptor<List<CardmarketPriceCandidate>> matched = ArgumentCaptor.captor();
        verify(cardmarketSingleMatcher).match(matched.capture(), any(), any(), any());
        assertThat(matched.getValue()).containsExactly(english);
        verify(cardmarketPriceImportService).importPricingSnapshot(List.of(), List.of(), List.of(english));

        // The expansion matcher still sees it, so the excluded expansion keeps a row of its own.
        verify(cardmarketExpansionMatcher).match(any(), eq(List.of(english, japanese)), any(), eq(List.of(2002L)), any());
    }

    @Test
    void syncPrices_keepsExistingHistoryWhenNoCandidatesWereMapped() {
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of());

        assertThatThrownBy(() -> service().syncPrices())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("keeping the existing snapshot");
        verify(cardmarketPriceImportService, never()).importPricingSnapshot(any(), any(), any());
    }

    @Test
    void syncPrices_keepsExistingMappingsWhenTheCatalogExposesNoSingles() {
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of(candidate(101L, 1001L)));
        when(cardmarketPriceApiClient.fetchNonEnglishExpansionIds()).thenReturn(List.of());
        when(priceableSingleCatalogClient.fetchPriceableSingles()).thenReturn(List.of());

        assertThatThrownBy(() -> service().syncPrices())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("keeping the existing mappings");
        verify(cardmarketPriceImportService, never()).importPricingSnapshot(any(), any(), any());
    }

    @Test
    void syncPrices_keepsExistingSnapshotWhenEveryProductWasClassifiedAsNonEnglish() {
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of(candidate(202L, 2002L)));
        when(cardmarketPriceApiClient.fetchNonEnglishExpansionIds()).thenReturn(List.of(2002L));

        assertThatThrownBy(() -> service().syncPrices())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-English");
        verify(cardmarketPriceImportService, never()).importPricingSnapshot(any(), any(), any());
    }

    @Test
    void syncPrices_reusesAnAlreadyStoredGuideWhileRebuildingMappings() {
        var candidate = candidate(101L, 1001L);
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of(candidate));
        when(cardmarketPriceApiClient.fetchNonEnglishExpansionIds()).thenReturn(List.of());
        when(cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(GUIDE_CREATED_AT)).thenReturn(true);
        when(priceableSingleCatalogClient.fetchPriceableSingles()).thenReturn(List.of(single()));
        when(cardmarketExpansionRepository.findAll()).thenReturn(List.of());
        when(cardmarketExpansionMatcher.match(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(cardmarketSingleMatcher.match(any(), any(), any(), any())).thenReturn(List.of());

        service().syncPrices();

        verify(priceableSingleCatalogClient).fetchPriceableSingles();
        verify(cardmarketPriceImportService).importPricingSnapshot(List.of(), List.of(), List.of());
        assertThat(candidate.getLastSyncedAt()).isNull();
    }

    private CardmarketPriceSyncService service() {
        return new CardmarketPriceSyncService(
                cardmarketPriceApiClient,
                priceableSingleCatalogClient,
                cardmarketPriceCandidateRepository,
                cardmarketExpansionRepository,
                cardmarketExpansionMatcher,
                cardmarketSingleMatcher,
                cardmarketPriceImportService);
    }
}
