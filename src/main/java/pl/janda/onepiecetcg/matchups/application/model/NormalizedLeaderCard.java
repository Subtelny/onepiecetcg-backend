package pl.janda.onepiecetcg.matchups.application.model;

import java.math.BigDecimal;

public record NormalizedLeaderCard(
        String leaderCode,
        String cardCode,
        MatchupLeaderCardCategory category,
        BigDecimal inclusionRate,
        BigDecimal typicalCopies
) {
}
