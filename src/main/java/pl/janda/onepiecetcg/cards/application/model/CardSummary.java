package pl.janda.onepiecetcg.cards.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    @Builder.Default
    private boolean released = true;

    private LocalDate releaseDate;

    private String flatRarity;

    private String cardImage;

    private String variantIndex;

    private String priceReference;
}
