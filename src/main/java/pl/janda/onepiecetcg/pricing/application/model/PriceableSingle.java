package pl.janda.onepiecetcg.pricing.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceableSingle {

    private String priceReference;

    private String sourceCardId;

    private String cardCode;

    private String releaseId;

    private String releaseName;

    private String setName;

    private String variantIndex;
}
