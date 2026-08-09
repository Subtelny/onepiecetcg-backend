package pl.janda.onepiecetcg.matchups.application.model;

import java.math.BigDecimal;

public record NormalizedMatchup(
        String leaderCode,
        String opponentCode,
        Long games,
        BigDecimal winRate,
        BigDecimal firstWinRate,
        BigDecimal secondWinRate,
        Long firstGames,
        Long secondGames
) {
}
