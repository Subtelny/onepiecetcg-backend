package pl.janda.onepiecetcg.application.model;

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

    private String flatRarity;

    private String cardImage;
}
