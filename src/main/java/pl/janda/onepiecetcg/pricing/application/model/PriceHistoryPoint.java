package pl.janda.onepiecetcg.pricing.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * A single point of a price series: one observation whose trend or low price differs from the
 * previous observation. Days without a point repeat the previous point's prices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryPoint {

    private PriceSource source;

    private String currency;

    private OffsetDateTime observedAt;

    private BigDecimal trendPrice;

    private BigDecimal lowPrice;
}
