package pl.janda.onepiecetcg.pricing.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardmarketProductPageRequest {

    private Long productId;

    private Long expansionId;
}
