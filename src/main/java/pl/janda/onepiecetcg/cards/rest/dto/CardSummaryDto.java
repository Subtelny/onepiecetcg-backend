package pl.janda.onepiecetcg.cards.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSummaryDto {
    private String id;
    private String name;
    private String displayName;
    private String sourceProduct;
    private String cardNumber;
    private String flatRarity;
    private String imageUrl;
    private String variantIndex;
    private List<CardPriceDto> prices;
}
