package pl.janda.onepiecetcg.deckbuilder.rest.mapper;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.application.model.CardSummary;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeckBuilderCardMapperTest {

    private final DeckBuilderCardMapper mapper = new DeckBuilderCardMapper();

    @Test
    void toDto_mapsCoreFieldsAndParsesColorAndAttributeTokens() {
        var card = SetCard.builder()
                .id(1L)
                .cardSetId("OP10-009")
                .cardName("Monkey.D.Luffy")
                .displayName("Monkey.D.Luffy (Winner)")
                .sourceProduct("Winner Pack 2026 Vol. 2")
                .released(false)
                .releaseDate(LocalDate.of(2026, 8, 28))
                .variantIndex("p1")
                .cardType("LEADER")
                .cardColor("red, green")
                .cardCost("5")
                .cardPower("5000")
                .counterAmount(1000)
                .attribute("Slash, Strike")
                .cardText("Some effect text")
                .rarity("L")
                .flatRarity("L")
                .cardImage("https://example.com/card.png")
                .marketPrice(10.5)
                .inventoryPrice(9.0)
                .build();

        var dto = mapper.toDto(card);

        assertThat(dto.getId()).isEqualTo("1");
        assertThat(dto.getName()).isEqualTo("Monkey.D.Luffy");
        assertThat(dto.getDisplayName()).isEqualTo("Monkey.D.Luffy (Winner)");
        assertThat(dto.getSourceProduct()).isEqualTo("Winner Pack 2026 Vol. 2");
        assertThat(dto.isReleased()).isFalse();
        assertThat(dto.getReleaseDate()).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(dto.getVariantIndex()).isEqualTo("p1");
        assertThat(dto.getType()).isEqualTo("LEADER");
        assertThat(dto.getColor()).containsExactly("RED", "GREEN");
        assertThat(dto.getCost()).isEqualTo(5);
        assertThat(dto.getPower()).isEqualTo(5000);
        assertThat(dto.getCounter()).isEqualTo(1000);
        assertThat(dto.getAttribute()).containsExactly("Slash", "Strike");
        assertThat(dto.getEffect()).isEqualTo("Some effect text");
        assertThat(dto.getRarity()).isEqualTo("L");
        assertThat(dto.getFlatRarity()).isEqualTo("L");
        assertThat(dto.getCardNumber()).isEqualTo("OP10-009");
        assertThat(dto.getImageUrl()).isEqualTo("https://example.com/card.png");
        assertThat(dto.getMarketPrice()).isEqualTo(10.5);
        assertThat(dto.getInventoryPrice()).isEqualTo(9.0);
    }

    @Test
    void toDto_returnsNullForNullInput() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDtoList_mapsEachCard() {
        var card = SetCard.builder().id(1L).cardSetId("OP10-009").cardName("Luffy").build();

        var dtos = mapper.toDtoList(List.of(card));

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getCardNumber()).isEqualTo("OP10-009");
    }

    @Test
    void toSummaryDto_mapsSummaryFieldsOnly() {
        var summary = CardSummary.builder()
                .id(2L)
                .cardSetId("OP13-119")
                .cardName("Charlotte Katakuri")
                .displayName("Charlotte Katakuri (Winner)")
                .sourceProduct("Winner Pack 2026 Vol. 1")
                .variantIndex("p2")
                .flatRarity("SR")
                .cardImage("https://example.com/katakuri.png")
                .build();

        var dto = mapper.toSummaryDto(summary);

        assertThat(dto.getId()).isEqualTo("2");
        assertThat(dto.getName()).isEqualTo("Charlotte Katakuri");
        assertThat(dto.getDisplayName()).isEqualTo("Charlotte Katakuri (Winner)");
        assertThat(dto.getSourceProduct()).isEqualTo("Winner Pack 2026 Vol. 1");
        assertThat(dto.getVariantIndex()).isEqualTo("p2");
        assertThat(dto.getCardNumber()).isEqualTo("OP13-119");
        assertThat(dto.getFlatRarity()).isEqualTo("SR");
        assertThat(dto.getImageUrl()).isEqualTo("https://example.com/katakuri.png");
    }

    @Test
    void toFilterOptionsDto_mapsSetsToDedicatedSetOptionDto() {
        var options = CardFilterOptions.builder()
                .types(List.of("LEADER"))
                .colors(List.of("RED"))
                .rarities(List.of("L"))
                .flatRarities(List.of("L"))
                .costs(List.of("5"))
                .sets(List.of(CardSet.builder().setId("OP10").setName("Rise of the New King").build()))
                .attributes(List.of("Slash"))
                .attributeCombos(List.of())
                .subTypes(List.of("Straw Hat Crew"))
                .prefixes(List.of("OP10"))
                .build();

        var dto = mapper.toFilterOptionsDto(options);

        assertThat(dto.getTypes()).containsExactly("LEADER");
        assertThat(dto.getSets()).hasSize(1);
        assertThat(dto.getSets().get(0).getSetId()).isEqualTo("OP10");
        assertThat(dto.getSets().get(0).getSetName()).isEqualTo("Rise of the New King");
    }
}
