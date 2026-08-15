package pl.janda.onepiecetcg.pricing.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardmarketProductPage {

    private Long productId;

    private Long expansionId;

    private String expansionSlug;

    private Integer version;
}
