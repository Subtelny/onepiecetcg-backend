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
public class CardPriceHistoryPointDto {

    private String source;

    private String currency;

    private String observedAt;

    private BigDecimal trendPrice;

    private BigDecimal lowPrice;
}
