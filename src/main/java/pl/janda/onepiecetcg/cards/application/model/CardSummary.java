package pl.janda.onepiecetcg.cards.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardSummary {

    private Long id;

    private String cardSetId;

    private String cardName;

    private String displayName;

    private String sourceProduct;

    private String flatRarity;

    private String cardImage;

    private String variantIndex;

    private String priceReference;
}
