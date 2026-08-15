package pl.janda.onepiecetcg.pricing.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record CardmarketPriceResponse(
        Long idProduct,
        BigDecimal avg,
        BigDecimal low,
        BigDecimal trend,
        BigDecimal avg1,
        BigDecimal avg7,
        BigDecimal avg30,
        @JsonProperty("avg-foil") BigDecimal avgFoil,
        @JsonProperty("low-foil") BigDecimal lowFoil,
        @JsonProperty("trend-foil") BigDecimal trendFoil,
        @JsonProperty("avg1-foil") BigDecimal avg1Foil,
        @JsonProperty("avg7-foil") BigDecimal avg7Foil,
        @JsonProperty("avg30-foil") BigDecimal avg30Foil
) {
}
