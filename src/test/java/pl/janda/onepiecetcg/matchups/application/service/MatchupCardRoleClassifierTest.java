package pl.janda.onepiecetcg.matchups.application.service;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCardCategory;
import pl.janda.onepiecetcg.matchups.application.model.NormalizedLeaderCard;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MatchupCardRoleClassifierTest {

    private final MatchupCardRoleClassifier classifier = new MatchupCardRoleClassifier();

    @Test
    void classify_marksGenericTwoKCounterAsTechEvenWhenItAppearsInEveryDecklist() {
        var profile = profile(MatchupLeaderCardCategory.EXPECTED, "100.00", "3.0");
        var leader = leader();
        var usoHachi = SetCard.builder()
                .cardType("Character")
                .cardCost("3")
                .subTypes("Straw Hat Crew")
                .counterAmount(2000)
                .cardText("[On Play] If you have 8 or more DON!! cards, rest an opponent's Character.")
                .build();

        var result = classifier.classify(profile, leader, usoHachi);

        assertThat(result).isEqualTo(MatchupLeaderCardCategory.POSSIBLE_TECH);
    }

    @Test
    void classify_keepsArchetypeCardInCoreAtAFlexibleCopyCount() {
        var profile = profile(MatchupLeaderCardCategory.EXPECTED, "100.00", "2.0");
        var leader = leader();
        var rosinante = SetCard.builder()
                .cardType("Character")
                .cardCost("5")
                .subTypes("Navy Donquixote Pirates")
                .counterAmount(1000)
                .cardText("[On Play] You may trash 1 Event from your hand: Draw 2 cards.")
                .build();

        var result = classifier.classify(profile, leader, rosinante);

        assertThat(result).isEqualTo(MatchupLeaderCardCategory.EXPECTED);
    }

    @Test
    void classify_keepsGenericFullPlaysetAndRepeatedFinisherInCore() {
        var leader = leader();
        var genericEvent = SetCard.builder().cardType("Event").subTypes("Foxy Pirates").build();
        var genericFinisher = SetCard.builder()
                .cardType("Character")
                .cardCost("10")
                .subTypes("Big Mom Pirates")
                .build();

        assertThat(classifier.classify(
                profile(MatchupLeaderCardCategory.EXPECTED, "80.00", "4.0"), leader, genericEvent))
                .isEqualTo(MatchupLeaderCardCategory.EXPECTED);
        assertThat(classifier.classify(
                profile(MatchupLeaderCardCategory.EXPECTED, "80.00", "2.0"), leader, genericFinisher))
                .isEqualTo(MatchupLeaderCardCategory.EXPECTED);
    }

    @Test
    void classify_neverPromotesLowInclusionTechBasedOnCardRoleAlone() {
        var result = classifier.classify(
                profile(MatchupLeaderCardCategory.POSSIBLE_TECH, "40.00", "4.0"),
                leader(),
                SetCard.builder().cardType("Character").subTypes("Donquixote Pirates").build());

        assertThat(result).isEqualTo(MatchupLeaderCardCategory.POSSIBLE_TECH);
    }

    @Test
    void classify_keepsConfidentTechButDoesNotConfirmCoreForAnInsufficientSample() {
        var observed = profile(MatchupLeaderCardCategory.OBSERVED, "100.00", "3.0");
        var usoHachi = SetCard.builder()
                .cardType("Character")
                .cardCost("3")
                .subTypes("Straw Hat Crew")
                .counterAmount(2000)
                .build();
        var archetypeCard = SetCard.builder()
                .cardType("Character")
                .cardCost("5")
                .subTypes("Donquixote Pirates")
                .counterAmount(1000)
                .build();

        assertThat(classifier.classify(observed, leader(), usoHachi))
                .isEqualTo(MatchupLeaderCardCategory.POSSIBLE_TECH);
        assertThat(classifier.classify(observed, leader(), archetypeCard))
                .isEqualTo(MatchupLeaderCardCategory.OBSERVED);
    }

    private NormalizedLeaderCard profile(MatchupLeaderCardCategory category, String inclusionRate,
                                         String typicalCopies) {
        return new NormalizedLeaderCard("OP14-060", "ST18-001", category,
                new BigDecimal(inclusionRate), new BigDecimal(typicalCopies), 3);
    }

    private SetCard leader() {
        return SetCard.builder()
                .cardType("Leader")
                .subTypes("The Seven Warlords of the Sea Donquixote Pirates")
                .cardText("Select your Leader or 1 of your {Donquixote Pirates} type Characters.")
                .build();
    }
}
