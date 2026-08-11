package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardDisplayNameServiceTest {

    private final CardDisplayNameService service = new CardDisplayNameService();

    private static SetCard card(String cardSetId, String cardName, String variantIndex, String sourceProduct) {
        return SetCard.builder()
                .cardSetId(cardSetId)
                .cardName(cardName)
                .variantIndex(variantIndex)
                .sourceProduct(sourceProduct)
                .build();
    }

    @Test
    void assignDisplayNames_shortensTheOnlyWinnerVariantForACard() {
        var defaultCard = card("OP01-016", "Nami", "0", "-ROMANCE DAWN- [OP-01]");
        var winner = card("OP01-016", "Nami", "p1", "Winner Pack 2026 Vol. 2");

        service.assignDisplayNames(List.of(defaultCard, winner));

        assertThat(defaultCard.getDisplayName()).isEqualTo("Nami");
        assertThat(winner.getDisplayName()).isEqualTo("Nami (Winner)");
    }

    @Test
    void assignDisplayNames_usesFullProductNamesWhenACardHasMultipleWinnerVariants() {
        var firstWinner = card("OP01-016", "Nami", "p1", "Winner Pack 2026 Vol. 1");
        var secondWinner = card("OP01-016", "Nami", "p2", "Winner Pack 2026 Vol. 2");

        service.assignDisplayNames(List.of(firstWinner, secondWinner));

        assertThat(firstWinner.getDisplayName()).isEqualTo("Nami (Winner Pack 2026 Vol. 1)");
        assertThat(secondWinner.getDisplayName()).isEqualTo("Nami (Winner Pack 2026 Vol. 2)");
    }

    @Test
    void assignDisplayNames_addsVariantIndexWhenTheExactProductIsRepeated() {
        var firstWinner = card("OP01-016", "Nami", "p1", "Winner Pack 2026 Vol. 2");
        var secondWinner = card("OP01-016", "Nami", "p2", " winner pack 2026 vol. 2 ");

        service.assignDisplayNames(List.of(firstWinner, secondWinner));

        assertThat(firstWinner.getDisplayName()).isEqualTo("Nami (Winner Pack 2026 Vol. 2) [p1]");
        assertThat(secondWinner.getDisplayName()).isEqualTo("Nami (winner pack 2026 vol. 2) [p2]");
    }

    @Test
    void assignDisplayNames_usesFullNonWinnerProductAndFallsBackToVariantIndex() {
        var productVariant = card("OP01-016", "Nami", "p1", "Premium Card Collection");
        var unknownProductVariant = card("OP01-016", "Nami", "r1", null);

        service.assignDisplayNames(List.of(productVariant, unknownProductVariant));

        assertThat(productVariant.getDisplayName()).isEqualTo("Nami (Premium Card Collection)");
        assertThat(unknownProductVariant.getDisplayName()).isEqualTo("Nami [r1]");
    }
}
