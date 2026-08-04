package pl.janda.onepiecetcg.cards.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSummaryDto {
    private String id;
    private String name;
    private String cardNumber;
    private String flatRarity;
    private String imageUrl;
    private Integer variantIndex;
}
