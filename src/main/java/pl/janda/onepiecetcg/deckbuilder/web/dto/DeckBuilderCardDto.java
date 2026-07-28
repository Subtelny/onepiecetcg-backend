package pl.janda.onepiecetcg.deckbuilder.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckBuilderCardDto {
    private String id;
    private String name;
    private String type;
    private List<String> color;
    private Integer cost;
    private Integer power;
    private Integer counter;
    private List<String> attribute;
    private String effect;
    private String rarity;
    private String flatRarity;
    private String cardNumber;
    private String imageUrl;
    private Double marketPrice;
    private Double inventoryPrice;
}
