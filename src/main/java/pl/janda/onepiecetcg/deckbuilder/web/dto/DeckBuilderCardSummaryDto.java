package pl.janda.onepiecetcg.deckbuilder.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckBuilderCardSummaryDto {
    private String id;
    private String name;
    private String cardNumber;
    private String flatRarity;
    private String imageUrl;
}
