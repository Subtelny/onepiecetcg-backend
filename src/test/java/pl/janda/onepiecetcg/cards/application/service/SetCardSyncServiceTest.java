package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCard;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetCardSyncServiceTest {

    @Mock
    private OnePieceCardRepository onePieceCardRepository;

    @Mock
    private FlatRarityCalculatorService flatRarityCalculatorService;

    @Mock
    private SetCardReplacementService setCardReplacementService;

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void syncSetCards_mapsSourceVariantsAndLeaderFields() {
        var source = OnePieceCard.builder()
                .id("OP01-001_p1")
                .baseId("OP01-001")
                .name("Roronoa Zoro")
                .setId("OP-01")
                .setName("BOOSTER PACK -ROMANCE DAWN- [OP-01]")
                .rarity("Leader")
                .category("Leader")
                .imageUrl("https://example.test/OP01-001_p1.png")
                .colors("Red/Green")
                .cost(5)
                .power(5000)
                .attributes("Slash")
                .types("Supernovas,Straw Hat Crew")
                .sourceProduct("Winner Pack 2026 Vol. 2")
                .effect("[Activate: Main] Do something.")
                .trigger("Draw 1 card.")
                .build();
        when(onePieceCardRepository.findAll()).thenReturn(List.of(source));
        var service = new SetCardSyncService(
                onePieceCardRepository, flatRarityCalculatorService, new CardDisplayNameService(),
                setCardReplacementService);

        service.syncSetCards();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(flatRarityCalculatorService).assignFlatRarities(captor.capture());
        var mapped = ((List<SetCard>) captor.getValue()).getFirst();
        assertThat(mapped.getCardSetId()).isEqualTo("OP01-001");
        assertThat(mapped.getCardPrefix()).isEqualTo("OP01");
        assertThat(mapped.getSetId()).isEqualTo("OP-01");
        assertThat(mapped.getSetName()).isEqualTo("BOOSTER PACK -ROMANCE DAWN- [OP-01]");
        assertThat(mapped.getRarity()).isEqualTo("L");
        assertThat(mapped.getCardColor()).isEqualTo("Red Green");
        assertThat(mapped.getCardType()).isEqualTo("Leader");
        assertThat(mapped.getLife()).isEqualTo("5");
        assertThat(mapped.getCardCost()).isNull();
        assertThat(mapped.getCardPower()).isEqualTo("5000");
        assertThat(mapped.getSubTypes()).isEqualTo("Supernovas Straw Hat Crew");
        assertThat(mapped.getCardText()).isEqualTo("[Activate: Main] Do something.\n[Trigger] Draw 1 card.");
        assertThat(mapped.getCardImageId()).isEqualTo("OP01-001_p1");
        assertThat(mapped.getSourceProduct()).isEqualTo("Winner Pack 2026 Vol. 2");
        assertThat(mapped.getDisplayName()).isEqualTo("Roronoa Zoro (Winner)");
        assertThat(mapped.getLastSyncedAt()).isNotNull();
        assertThat(mapped.getVariantIndex()).isEqualTo("p1");
        verify(setCardReplacementService).replaceAll(captor.getValue());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void syncSetCards_marksPromotionTableCardsAsPromo() {
        var source = OnePieceCard.builder()
                .id("OP01-001_p9")
                .baseId("OP01-001")
                .setId("569901")
                .setName("Promotion card")
                .rarity("Special")
                .category("Character")
                .cost(3)
                .build();
        when(onePieceCardRepository.findAll()).thenReturn(List.of(source));
        var service = new SetCardSyncService(
                onePieceCardRepository, flatRarityCalculatorService, new CardDisplayNameService(),
                setCardReplacementService);

        service.syncSetCards();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(setCardReplacementService).replaceAll(captor.capture());
        var mapped = ((List<SetCard>) captor.getValue()).getFirst();
        assertThat(mapped.getRarity()).isEqualTo("PR");
        assertThat(mapped.getFlatRarity()).isEqualTo("SP");
        assertThat(mapped.getCardCost()).isEqualTo("3");
        assertThat(mapped.getLife()).isNull();
        assertThat(mapped.getVariantIndex()).isEqualTo("p9");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void syncSetCards_derivesDefaultAndReprintIndexesFromSourceIds() {
        when(onePieceCardRepository.findAll()).thenReturn(List.of(
                OnePieceCard.builder()
                        .id("OP16-079")
                        .setId("OP-16")
                        .build(),
                OnePieceCard.builder()
                        .id("OP16-079_r1")
                        .baseId("OP16-079")
                        .setId("OP-16")
                        .build()
        ));
        var service = new SetCardSyncService(
                onePieceCardRepository, flatRarityCalculatorService, new CardDisplayNameService(),
                setCardReplacementService);

        service.syncSetCards();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(setCardReplacementService).replaceAll(captor.capture());
        assertThat((List<SetCard>) captor.getValue())
                .extracting(SetCard::getVariantIndex)
                .containsExactly("0", "r1");
    }

    @Test
    void syncSetCards_refusesToTruncateTargetWhenSourceIsEmpty() {
        when(onePieceCardRepository.findAll()).thenReturn(List.of());
        var service = new SetCardSyncService(
                onePieceCardRepository, flatRarityCalculatorService, new CardDisplayNameService(),
                setCardReplacementService);

        assertThatThrownBy(service::syncSetCards)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refusing to replace set_cards");

        verify(flatRarityCalculatorService, never()).assignFlatRarities(org.mockito.ArgumentMatchers.any());
        verify(setCardReplacementService, never()).replaceAll(org.mockito.ArgumentMatchers.any());
    }
}
