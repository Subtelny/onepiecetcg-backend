package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.client.CardErrataApiClient;
import pl.janda.onepiecetcg.cards.application.model.CardErrata;
import pl.janda.onepiecetcg.cards.application.repository.CardErrataRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardErrataSyncServiceTest {

    @Mock
    private CardErrataRepository cardErrataRepository;

    @Mock
    private CardErrataApiClient cardErrataApiClient;

    private CardErrataSyncService cardErrataSyncService;

    @Test
    void syncErrata_fetchesThenDeletesThenSaves_andStampsLastSyncedAt() {
        cardErrataSyncService = new CardErrataSyncService(cardErrataRepository, cardErrataApiClient);
        var errata = CardErrata.builder()
                .cardCode("OP13-119")
                .noticeDate(LocalDate.of(2024, 1, 1))
                .beforeText("before")
                .afterText("after")
                .build();
        when(cardErrataApiClient.fetchAllErrata()).thenReturn(List.of(errata));

        cardErrataSyncService.syncErrata();

        var order = inOrder(cardErrataApiClient, cardErrataRepository);
        order.verify(cardErrataApiClient).fetchAllErrata();
        order.verify(cardErrataRepository).deleteAll();
        order.verify(cardErrataRepository).saveAll(List.of(errata));

        assertThat(errata.getLastSyncedAt()).isNotNull();
    }
}
