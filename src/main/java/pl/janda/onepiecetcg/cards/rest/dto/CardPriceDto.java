package pl.janda.onepiecetcg.cards.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardPriceDto {

    private String source;

    private String currency;

    private String productId;

    private String productName;

    private BigDecimal averagePrice;

    private BigDecimal lowPrice;

    private BigDecimal trendPrice;

    private String observedAt;
}
