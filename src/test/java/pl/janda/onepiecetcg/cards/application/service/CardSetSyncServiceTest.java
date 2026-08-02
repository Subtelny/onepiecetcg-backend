package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCardSet;
import pl.janda.onepiecetcg.cards.application.repository.CardSetRepository;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardSetRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardSetSyncServiceTest {

    @Mock
    private CardSetRepository cardSetRepository;

    @Mock
    private OnePieceCardSetRepository onePieceCardSetRepository;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void syncCardSets_mapsAndSavesTheSourceTable() {
        var source = OnePieceCardSet.builder()
                .setId("OP-01")
                .label("BOOSTER PACK -ROMANCE DAWN- [OP-01]")
                .build();
        when(onePieceCardSetRepository.findAll()).thenReturn(List.of(source));
        when(cardSetRepository.findAll()).thenReturn(List.of());
        var service = new CardSetSyncService(cardSetRepository, onePieceCardSetRepository);

        var newSetsFound = service.syncCardSets();

        assertThat(newSetsFound).isTrue();
        var captor = ArgumentCaptor.forClass(Iterable.class);
        verify(cardSetRepository).saveAll(captor.capture());
        assertThat((Iterable<CardSet>) captor.getValue()).singleElement().satisfies(cardSet -> {
            assertThat(cardSet.getSetId()).isEqualTo("OP-01");
            assertThat(cardSet.getSetName()).isEqualTo("BOOSTER PACK -ROMANCE DAWN- [OP-01]");
            assertThat(cardSet.getLastSyncedAt()).isNotNull();
        });
    }

    @Test
    void syncCardSets_skipsWhenTargetAlreadyMatchesSource() {
        var source = OnePieceCardSet.builder().setId("OP-01").label("Romance Dawn").build();
        var target = CardSet.builder().setId("OP-01").setName("Romance Dawn").build();
        when(onePieceCardSetRepository.findAll()).thenReturn(List.of(source));
        when(cardSetRepository.findAll()).thenReturn(List.of(target));
        var service = new CardSetSyncService(cardSetRepository, onePieceCardSetRepository);

        assertThat(service.syncCardSets()).isFalse();

        verify(cardSetRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void syncCardSets_doesNotOverwriteTargetWhenSourceIsEmpty() {
        when(onePieceCardSetRepository.findAll()).thenReturn(List.of());
        var service = new CardSetSyncService(cardSetRepository, onePieceCardSetRepository);

        assertThat(service.syncCardSets()).isFalse();

        verify(cardSetRepository, never()).findAll();
        verify(cardSetRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }
}
