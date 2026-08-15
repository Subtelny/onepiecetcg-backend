package pl.janda.onepiecetcg.deckbuilder.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckBuilderCardPriceDto {

    private String source;

    private String currency;

    private String productId;

    private String productName;

    private BigDecimal averagePrice;

    private BigDecimal lowPrice;

    private BigDecimal trendPrice;

    private BigDecimal averagePrice1Day;

    private BigDecimal averagePrice7Days;

    private BigDecimal averagePrice30Days;

    private BigDecimal foilAveragePrice;

    private BigDecimal foilLowPrice;

    private BigDecimal foilTrendPrice;

    private BigDecimal foilAveragePrice1Day;

    private BigDecimal foilAveragePrice7Days;

    private BigDecimal foilAveragePrice30Days;

    private String observedAt;
}
