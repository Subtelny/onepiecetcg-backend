package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCard;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                .effect("[Activate: Main] Do something.")
                .trigger("Draw 1 card.")
                .scrapedAt(OffsetDateTime.parse("2026-08-03T00:05:36+02:00"))
                .build();
        when(onePieceCardRepository.findAll()).thenReturn(List.of(source));
        var service = new SetCardSyncService(
                onePieceCardRepository, flatRarityCalculatorService, setCardReplacementService);

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
        assertThat(mapped.getDateScraped()).isEqualTo("2026-08-03T00:05:36+02:00");
        assertThat(mapped.getLastSyncedAt()).isNotNull();
        assertThat(mapped.isPromo()).isFalse();
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
                onePieceCardRepository, flatRarityCalculatorService, setCardReplacementService);

        service.syncSetCards();

        var captor = ArgumentCaptor.forClass(List.class);
        verify(setCardReplacementService).replaceAll(captor.capture());
        var mapped = ((List<SetCard>) captor.getValue()).getFirst();
        assertThat(mapped.isPromo()).isTrue();
        assertThat(mapped.getRarity()).isEqualTo("PR");
        assertThat(mapped.getFlatRarity()).isEqualTo("SP");
        assertThat(mapped.getCardCost()).isEqualTo("3");
        assertThat(mapped.getLife()).isNull();
    }

    @Test
    void syncSetCards_refusesToTruncateTargetWhenSourceIsEmpty() {
        when(onePieceCardRepository.findAll()).thenReturn(List.of());
        var service = new SetCardSyncService(
                onePieceCardRepository, flatRarityCalculatorService, setCardReplacementService);

        assertThatThrownBy(service::syncSetCards)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refusing to replace set_cards");

        verify(flatRarityCalculatorService, never()).assignFlatRarities(org.mockito.ArgumentMatchers.any());
        verify(setCardReplacementService, never()).replaceAll(org.mockito.ArgumentMatchers.any());
    }
}
