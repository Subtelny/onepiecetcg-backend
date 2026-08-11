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
    void assignDisplayNames_usesFullUnsimplifiedProductAndFallsBackToVariantIndex() {
        var productVariant = card("OP01-016", "Nami", "p1", "Treasure Cup 2025");
        var unknownProductVariant = card("OP01-016", "Nami", "r1", null);

        service.assignDisplayNames(List.of(productVariant, unknownProductVariant));

        assertThat(productVariant.getDisplayName()).isEqualTo("Nami (Treasure Cup 2025)");
        assertThat(unknownProductVariant.getDisplayName()).isEqualTo("Nami [r1]");
    }

    @Test
    void assignDisplayNames_simplifiesKnownProductNames() {
        var tournamentKit = card("OP01-001", "Luffy", "p1", "Tournament Kit 2025 Vol.2");
        var tournamentPack = card("OP01-002", "Zoro", "p1", "Tournament Pack Vol.2");
        var premiumCollection = card(
                "OP01-003", "Nami", "p1", "Premium Card Collection -FILM RED Edition-");
        var celebrationPack = card("OP01-004", "Usopp", "p1", "CS 25-26 Celebration Pack");
        var releaseEvent = card("OP01-005", "Sanji", "p1", "ST15-20 Release Event");

        service.assignDisplayNames(List.of(
                tournamentKit, tournamentPack, premiumCollection, celebrationPack, releaseEvent));

        assertThat(tournamentKit.getDisplayName()).isEqualTo("Luffy (Tournament Kit)");
        assertThat(tournamentPack.getDisplayName()).isEqualTo("Zoro (Tournament Pack)");
        assertThat(premiumCollection.getDisplayName()).isEqualTo("Nami (Premium Card Collection)");
        assertThat(celebrationPack.getDisplayName()).isEqualTo("Usopp (Celebration Pack)");
        assertThat(releaseEvent.getDisplayName()).isEqualTo("Sanji (Release Event)");
    }

    @Test
    void assignDisplayNames_addsIndexesWhenSimplifiedProductLabelIsRepeated() {
        var firstKit = card("OP01-016", "Nami", "p1", "Tournament Kit 2024 Vol.1");
        var secondKit = card("OP01-016", "Nami", "p2", "Tournament Kit 2025 Vol.2");

        service.assignDisplayNames(List.of(firstKit, secondKit));

        assertThat(firstKit.getDisplayName()).isEqualTo("Nami (Tournament Kit) [p1]");
        assertThat(secondKit.getDisplayName()).isEqualTo("Nami (Tournament Kit) [p2]");
    }

    @Test
    void assignDisplayNames_simplifiesRegionalProductNamesAndAddsIndexesForRepeatedLabels() {
        var offlineParticipation = card(
                "OP02-001", "Luffy", "p1", "Offline Regional Participation Pack 2024 Vol. 2");
        var includedParticipation = card(
                "OP02-001", "Luffy", "p2", "Included in Online Regional Participation Pack Vol.1");
        var onlineParticipation = card(
                "OP02-001", "Luffy", "p3", "Online Regional Participation Pack 25-26 Season 1");
        var preRelease = card("OP02-002", "Zoro", "p1", "Pre-Release OP03");
        var offlineFinalist = card(
                "OP02-003", "Nami", "p1", "Offline Regional Finalist Card Set 25-26 Season 1");
        var onlineFinalist = card(
                "OP02-003", "Nami", "p2", "Online Regional Finalist Card Set 25-26 Season 1");
        var offlineChampion = card(
                "OP02-004", "Sanji", "p1", "Offline Regional Champion Card Set 25-26 Season 1");
        var onlineChampion = card(
                "OP02-004", "Sanji", "p2", "Online Regional Champion Card Set 25-26 Season 1");

        service.assignDisplayNames(List.of(
                offlineParticipation, includedParticipation, onlineParticipation, preRelease,
                offlineFinalist, onlineFinalist, offlineChampion, onlineChampion));

        assertThat(offlineParticipation.getDisplayName()).isEqualTo("Luffy (Participation Pack) [p1]");
        assertThat(includedParticipation.getDisplayName()).isEqualTo("Luffy (Participation Pack) [p2]");
        assertThat(onlineParticipation.getDisplayName()).isEqualTo("Luffy (Participation Pack) [p3]");
        assertThat(preRelease.getDisplayName()).isEqualTo("Zoro (Pre-Release)");
        assertThat(offlineFinalist.getDisplayName()).isEqualTo("Nami (Finalist) [p1]");
        assertThat(onlineFinalist.getDisplayName()).isEqualTo("Nami (Finalist) [p2]");
        assertThat(offlineChampion.getDisplayName()).isEqualTo("Sanji (Champion) [p1]");
        assertThat(onlineChampion.getDisplayName()).isEqualTo("Sanji (Champion) [p2]");
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
