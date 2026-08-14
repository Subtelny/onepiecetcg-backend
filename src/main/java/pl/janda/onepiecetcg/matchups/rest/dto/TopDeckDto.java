package pl.janda.onepiecetcg.matchups.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopDeckDto {

    private Integer totalCards;

    private Long games;

    private BigDecimal winRate;

    private List<MatchupCardDto> cards;
}
