package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.client.CardFaqApiClient;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.model.CardFaqListingEntry;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardFaqSyncServiceTest {

    @Mock
    private CardFaqRepository cardFaqRepository;

    @Mock
    private CardFaqApiClient cardFaqApiClient;

    @InjectMocks
    private CardFaqSyncService cardFaqSyncService;

    @Test
    void skipsSetWhenStoredPublishedDateAlreadyMatchesListing() {
        var publishedDate = LocalDate.of(2024, 4, 5);
        var entry = new CardFaqListingEntry("op01", publishedDate, "https://en.onepiece-cardgame.com/pdf/qa_op01.pdf?20240405");
        when(cardFaqApiClient.fetchFaqListing()).thenReturn(List.of(entry));
        when(cardFaqRepository.findBySetId("op01")).thenReturn(List.of(
                CardFaq.builder().setId("op01").publishedDate(publishedDate).build()));

        cardFaqSyncService.syncFaq();

        verify(cardFaqApiClient, never()).fetchFaqEntries(any(), any(), any());
        verify(cardFaqRepository, never()).deleteBySetId(any());
        verify(cardFaqRepository, never()).saveAll(any());
    }

    @Test
    void resyncsSetWhenPublishedDateChanged() {
        var oldDate = LocalDate.of(2024, 1, 1);
        var newDate = LocalDate.of(2024, 4, 5);
        var pdfUrl = "https://en.onepiece-cardgame.com/pdf/qa_op01.pdf?20240405";
        var entry = new CardFaqListingEntry("op01", newDate, pdfUrl);
        when(cardFaqApiClient.fetchFaqListing()).thenReturn(List.of(entry));
        when(cardFaqRepository.findBySetId("op01")).thenReturn(List.of(
                CardFaq.builder().setId("op01").publishedDate(oldDate).build()));

        var parsed = new ArrayList<CardFaq>();
        parsed.add(CardFaq.builder()
                .cardCode("OP01-001")
                .setId("op01")
                .question("Q")
                .answer("A")
                .publishedDate(newDate)
                .build());
        when(cardFaqApiClient.fetchFaqEntries("op01", newDate, pdfUrl)).thenReturn(parsed);

        cardFaqSyncService.syncFaq();

        var order = inOrder(cardFaqApiClient, cardFaqRepository);
        order.verify(cardFaqApiClient).fetchFaqListing();
        order.verify(cardFaqApiClient).fetchFaqEntries("op01", newDate, pdfUrl);
        order.verify(cardFaqRepository).deleteBySetId("op01");
        order.verify(cardFaqRepository).saveAll(parsed);

        assertThat(parsed.get(0).getLastSyncedAt()).isNotNull();
    }

    @Test
    void syncsNewSetWithNoExistingStoredRows() {
        var publishedDate = LocalDate.of(2026, 7, 17);
        var pdfUrl = "https://en.onepiece-cardgame.com/pdf/qa_st-36.pdf?20260717";
        var entry = new CardFaqListingEntry("st-36", publishedDate, pdfUrl);
        when(cardFaqApiClient.fetchFaqListing()).thenReturn(List.of(entry));
        when(cardFaqRepository.findBySetId("st-36")).thenReturn(List.of());
        when(cardFaqApiClient.fetchFaqEntries("st-36", publishedDate, pdfUrl)).thenReturn(List.of());

        cardFaqSyncService.syncFaq();

        verify(cardFaqRepository).deleteBySetId("st-36");
        verify(cardFaqRepository).saveAll(List.of());
    }
}
