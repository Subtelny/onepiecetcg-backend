package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.CardErrata;
import pl.janda.onepiecetcg.cards.application.repository.CardErrataRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardErrataServiceTest {

    @Mock
    private CardErrataRepository cardErrataRepository;

    private CardErrataService cardErrataService;

    @Test
    void listAll_delegatesToRepository() {
        cardErrataService = new CardErrataService(cardErrataRepository);
        var errata = List.of(errataFor("OP13-119", LocalDate.of(2024, 1, 1)));
        when(cardErrataRepository.findAll()).thenReturn(errata);

        var result = cardErrataService.listAll();

        assertThat(result).isEqualTo(errata);
    }

    @Test
    void historyByCardCodes_returnsEmptyMapWithoutQueryingRepository_whenNoCardCodesGiven() {
        cardErrataService = new CardErrataService(cardErrataRepository);

        var result = cardErrataService.historyByCardCodes(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(cardErrataRepository);
    }

    @Test
    void historyByCardCodes_returnsFullHistoryPerCardCodeSortedOldestToNewest() {
        cardErrataService = new CardErrataService(cardErrataRepository);
        var older = errataFor("OP13-119", LocalDate.of(2023, 6, 1));
        var newer = errataFor("OP13-119", LocalDate.of(2024, 1, 1));

        when(cardErrataRepository.findByCardCodeIn(List.of("OP13-119"))).thenReturn(List.of(newer, older));

        var result = cardErrataService.historyByCardCodes(Arrays.asList("OP13-119", "OP13-119", null));

        assertThat(result).containsOnlyKeys("OP13-119");
        assertThat(result.get("OP13-119")).containsExactly(older, newer);
        verify(cardErrataRepository).findByCardCodeIn(List.of("OP13-119"));
    }

    private CardErrata errataFor(String cardCode, LocalDate noticeDate) {
        return CardErrata.builder()
                .cardCode(cardCode)
                .noticeDate(noticeDate)
                .beforeText("before")
                .afterText("after")
                .build();
    }
}
