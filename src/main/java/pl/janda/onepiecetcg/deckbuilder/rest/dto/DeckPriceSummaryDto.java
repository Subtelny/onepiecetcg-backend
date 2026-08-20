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
public class DeckPriceSummaryDto {

    private List<DeckPriceTotalDto> totals;

    private Integer pricedCopies;

    private Integer totalCopies;
}
