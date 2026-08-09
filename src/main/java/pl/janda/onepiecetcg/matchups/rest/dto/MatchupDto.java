package pl.janda.onepiecetcg.matchups.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupDto {
    private String leaderCode;
    private String opponentCode;
    private Long games;
    private BigDecimal winRate;
    private BigDecimal firstWinRate;
    private BigDecimal secondWinRate;
    private Long firstGames;
    private Long secondGames;
}
