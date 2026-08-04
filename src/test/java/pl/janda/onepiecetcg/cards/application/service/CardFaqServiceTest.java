package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardFaqServiceTest {

    @Mock
    private CardFaqRepository cardFaqRepository;

    private CardFaqService cardFaqService;

    @Test
    void historyByCardCodes_returnsEmptyMapWithoutQueryingRepository_whenNoCardCodesGiven() {
        cardFaqService = new CardFaqService(cardFaqRepository);

        var result = cardFaqService.historyByCardCodes(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(cardFaqRepository);
    }

    @Test
    void historyByCardCodes_returnsFullHistoryPerCardCodeSortedOldestToNewest() {
        cardFaqService = new CardFaqService(cardFaqRepository);
        var older = faqFor("OP13-119", LocalDate.of(2023, 6, 1));
        var newer = faqFor("OP13-119", LocalDate.of(2024, 1, 1));

        when(cardFaqRepository.findByCardCodeIn(List.of("OP13-119"))).thenReturn(List.of(newer, older));

        var result = cardFaqService.historyByCardCodes(Arrays.asList("OP13-119", "OP13-119", null));

        assertThat(result).containsOnlyKeys("OP13-119");
        assertThat(result.get("OP13-119")).containsExactly(older, newer);
        verify(cardFaqRepository).findByCardCodeIn(List.of("OP13-119"));
    }

    private CardFaq faqFor(String cardCode, LocalDate publishedDate) {
        return CardFaq.builder()
                .cardCode(cardCode)
                .publishedDate(publishedDate)
                .question("question")
                .answer("answer")
                .build();
    }
}
