package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.client.CardmarketPriceApiClient;
import pl.janda.onepiecetcg.cards.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.cards.application.repository.CardmarketPriceCandidateRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardmarketPriceSyncServiceTest {

    private static final OffsetDateTime GUIDE_CREATED_AT =
            OffsetDateTime.parse("2026-08-01T02:00:00+02:00");

    @Mock
    private CardmarketPriceApiClient cardmarketPriceApiClient;

    @Mock
    private CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;

    @Mock
    private CardmarketPriceHistoryService cardmarketPriceHistoryService;

    @Test
    void syncPrices_appendsANewGuideWithoutDeletingHistory_andStampsCandidates() {
        var service = new CardmarketPriceSyncService(
                cardmarketPriceApiClient, cardmarketPriceCandidateRepository, cardmarketPriceHistoryService);
        var candidate = CardmarketPriceCandidate.builder()
                .productId(101L)
                .cardCode("OP01-001")
                .productName("Luffy (OP01-001)")
                .priceGuideVersion("2")
                .priceGuideCreatedAt(GUIDE_CREATED_AT)
                .build();
        var candidates = List.of(candidate);
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(candidates);
        when(cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(GUIDE_CREATED_AT))
                .thenReturn(false);

        service.syncPrices();

        var order = inOrder(cardmarketPriceApiClient, cardmarketPriceCandidateRepository, cardmarketPriceHistoryService);
        order.verify(cardmarketPriceApiClient).fetchPriceCandidates();
        order.verify(cardmarketPriceCandidateRepository).existsByPriceGuideCreatedAt(GUIDE_CREATED_AT);
        order.verify(cardmarketPriceHistoryService).appendAll(candidates);
        assertThat(candidate.getLastSyncedAt()).isNotNull();
    }

    @Test
    void syncPrices_keepsExistingSnapshotWhenNoCandidatesWereMapped() {
        var service = new CardmarketPriceSyncService(
                cardmarketPriceApiClient, cardmarketPriceCandidateRepository, cardmarketPriceHistoryService);
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of());

        assertThatThrownBy(service::syncPrices)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("keeping the existing snapshot");
        verify(cardmarketPriceHistoryService, never()).appendAll(List.of());
    }

    @Test
    void syncPrices_skipsAnAlreadyStoredDailyGuide() {
        var service = new CardmarketPriceSyncService(
                cardmarketPriceApiClient, cardmarketPriceCandidateRepository, cardmarketPriceHistoryService);
        var candidate = CardmarketPriceCandidate.builder()
                .productId(101L)
                .cardCode("OP01-001")
                .productName("Luffy (OP01-001)")
                .priceGuideVersion("2")
                .priceGuideCreatedAt(GUIDE_CREATED_AT)
                .build();
        when(cardmarketPriceApiClient.fetchPriceCandidates()).thenReturn(List.of(candidate));
        when(cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(GUIDE_CREATED_AT))
                .thenReturn(true);

        service.syncPrices();

        verify(cardmarketPriceHistoryService, never()).appendAll(List.of(candidate));
        assertThat(candidate.getLastSyncedAt()).isNull();
    }
}
