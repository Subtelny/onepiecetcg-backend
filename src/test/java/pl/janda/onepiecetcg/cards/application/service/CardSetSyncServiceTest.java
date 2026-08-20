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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
        var captor = ArgumentCaptor.forClass(List.class);
        verify(cardSetRepository).saveAll(captor.capture());
        assertThat((List<CardSet>) captor.getValue()).singleElement().satisfies(cardSet -> {
            assertThat(cardSet.getSetId()).isEqualTo("OP-01");
            assertThat(cardSet.getSetName()).isEqualTo("BOOSTER PACK -ROMANCE DAWN- [OP-01]");
            assertThat(cardSet.isReleased()).isTrue();
            assertThat(cardSet.getReleaseDate()).isNull();
            assertThat(cardSet.getLastSyncedAt()).isNotNull();
        });
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void syncCardSets_marksFutureCardKaizokuSetsAsUnreleased() {
        var releaseDate = LocalDate.of(2026, 8, 28);
        var source = OnePieceCardSet.builder()
                .setId("OP-17")
                .label("BOOSTER PACK [OP-17]")
                .released(false)
                .releaseDate(releaseDate)
                .build();
        when(onePieceCardSetRepository.findAll()).thenReturn(List.of(source));
        when(cardSetRepository.findAll()).thenReturn(List.of());
        var service = new CardSetSyncService(cardSetRepository, onePieceCardSetRepository);

        assertThat(service.syncCardSets()).isTrue();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(cardSetRepository).saveAll(captor.capture());
        assertThat((List<CardSet>) captor.getValue()).singleElement().satisfies(cardSet -> {
            assertThat(cardSet.isReleased()).isFalse();
            assertThat(cardSet.getReleaseDate()).isEqualTo(releaseDate);
        });
    }

    @Test
    void syncCardSets_replacesLeakMetadataWhenTheOfficialSetAppears() {
        var source = OnePieceCardSet.builder()
                .setId("OP-17")
                .label("Official OP-17")
                .build();
        var leaked = CardSet.builder()
                .setId("OP-17")
                .setName("Leaked OP-17")
                .released(false)
                .releaseDate(LocalDate.of(2026, 8, 28))
                .build();
        when(onePieceCardSetRepository.findAll()).thenReturn(List.of(source));
        when(cardSetRepository.findAll()).thenReturn(List.of(leaked));
        var service = new CardSetSyncService(cardSetRepository, onePieceCardSetRepository);

        service.syncCardSets();

        verify(cardSetRepository).saveAll(argThat(cardSets ->
                cardSets.size() == 1
                        && cardSets.getFirst().isReleased()
                        && cardSets.getFirst().getReleaseDate() == null
                        && cardSets.getFirst().getSetName().equals("Official OP-17")));
        verify(cardSetRepository, never()).deleteAll(any());
    }

    @Test
    void syncCardSets_removesExpiredLeakThatIsNoLongerInEitherSource() {
        var officialSource = OnePieceCardSet.builder().setId("OP-16").label("OP-16").build();
        var officialTarget = CardSet.builder().setId("OP-16").setName("OP-16").build();
        var expiredLeak = CardSet.builder()
                .setId("OP-17")
                .setName("OP-17 leak")
                .released(false)
                .build();
        when(onePieceCardSetRepository.findAll()).thenReturn(List.of(officialSource));
        when(cardSetRepository.findAll()).thenReturn(List.of(officialTarget, expiredLeak));
        var service = new CardSetSyncService(cardSetRepository, onePieceCardSetRepository);

        assertThat(service.syncCardSets()).isTrue();

        verify(cardSetRepository).deleteAll(List.of(expiredLeak));
        verify(cardSetRepository, never()).saveAll(any());
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
        verify(cardSetRepository, never()).deleteAll(org.mockito.ArgumentMatchers.any());
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
