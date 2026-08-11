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

    @Test
    void assignDisplayNames_usesAltAndReprintForCardSetProducts() {
        var alt = card("P-102", "Nami", "p1", "STARTER DECK -BLUE Kuzan- [ST-33]");
        var reprint = card("P-102", "Nami", "r1", "-THE TIME OF BATTLE- [OP-16]");

        service.assignDisplayNames(List.of(alt, reprint));

        assertThat(alt.getDisplayName()).isEqualTo("Nami (Alt)");
        assertThat(reprint.getDisplayName()).isEqualTo("Nami (Reprint)");
    }

    @Test
    void assignDisplayNames_addsIndexesWhenSeveralSetProductAltsShareACard() {
        var firstAlt = card("OP01-006", "Otama", "p3", "-ONE PIECE CARD THE BEST- [PRB-01]");
        var secondAlt = card("OP01-006", "Otama", "p4", "-ROMANCE DAWN- [OP01]");
        var reprint = card("OP01-006", "Otama", "r1", "-ONE PIECE CARD THE BEST- [PRB-01]");

        service.assignDisplayNames(List.of(firstAlt, secondAlt, reprint));

        assertThat(firstAlt.getDisplayName()).isEqualTo("Otama (Alt) [p3]");
        assertThat(secondAlt.getDisplayName()).isEqualTo("Otama (Alt) [p4]");
        assertThat(reprint.getDisplayName()).isEqualTo("Otama (Reprint)");
    }

    @Test
    void assignDisplayNames_addsIndexesWhenSeveralSetProductReprintsShareACard() {
        var firstReprint = card("OP03-003", "Izo", "r1", "-ONE PIECE CARD THE BEST- [PRB-01]");
        var secondReprint = card("OP03-003", "Izo", "r2", "-ONE PIECE CARD THE BEST vol.2- [PRB-02]");

        service.assignDisplayNames(List.of(firstReprint, secondReprint));

        assertThat(firstReprint.getDisplayName()).isEqualTo("Izo (Reprint) [r1]");
        assertThat(secondReprint.getDisplayName()).isEqualTo("Izo (Reprint) [r2]");
    }
}
