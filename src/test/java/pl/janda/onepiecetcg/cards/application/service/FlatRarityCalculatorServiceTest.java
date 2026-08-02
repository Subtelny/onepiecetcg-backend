package pl.janda.onepiecetcg.cards.application.service;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlatRarityCalculatorServiceTest {

    @Test
    void assignFlatRarities_preservesPhysicalRarityProvidedByTheSourceForPromoVariants() {
        var regular = SetCard.builder()
                .cardSetId("OP01-001")
                .cardPrefix("OP01")
                .rarity("L")
                .build();
        var promo = SetCard.builder()
                .cardSetId("OP01-001")
                .cardPrefix("OP01")
                .rarity("PR")
                .flatRarity("SP")
                .promo(true)
                .build();
        var service = new FlatRarityCalculatorService(new FlatRarityOverrideProperties());

        service.assignFlatRarities(List.of(regular, promo));

        assertThat(regular.getFlatRarity()).isEqualTo("L");
        assertThat(promo.getFlatRarity()).isEqualTo("SP");
    }
}
