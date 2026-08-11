package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.repository.SetCardCommandRepository;

import java.util.List;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class SetCardReplacementServiceTest {

    @Mock
    private SetCardCommandRepository repository;

    @Mock
    private CardFilterOptionService cardFilterOptionService;

    @InjectMocks
    private SetCardReplacementService service;

    @Test
    void replaceAll_locksTableBeforeDeletingAndSaving() {
        var cards = List.of(SetCard.builder().cardId("P-102").build());

        service.replaceAll(cards);

        InOrder ordered = inOrder(repository, cardFilterOptionService);
        ordered.verify(repository).lockForReplacement();
        ordered.verify(repository).deleteAll();
        ordered.verify(repository).saveAll(cards);
        ordered.verify(cardFilterOptionService).refresh();
    }
}
