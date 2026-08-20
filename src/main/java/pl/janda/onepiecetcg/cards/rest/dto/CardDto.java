package pl.janda.onepiecetcg.cards.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    private String id;
    private String name;
    private String displayName;
    private String sourceProduct;
    private boolean released;
    private LocalDate releaseDate;
    private String variantIndex;
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
    private List<CardPriceDto> prices;
    private List<CardPriceHistoryPointDto> priceHistory;
    private List<CardErrataEntryDto> errata;
    private List<CardFaqEntryDto> faq;
}
