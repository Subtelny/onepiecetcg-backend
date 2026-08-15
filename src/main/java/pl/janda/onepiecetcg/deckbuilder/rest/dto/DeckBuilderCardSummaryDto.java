package pl.janda.onepiecetcg.deckbuilder.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckBuilderCardSummaryDto {
    private String id;
    private String name;
    private String displayName;
    private String sourceProduct;
    private String variantIndex;
    private String cardNumber;
    private String flatRarity;
    private String imageUrl;
    private List<DeckBuilderCardPriceDto> prices;
}
