package pl.janda.onepiecetcg.matchups.application.model;

import java.math.BigDecimal;

public record NormalizedLeaderStat(
        String cardCode,
        Long matches,
        BigDecimal winRate,
        BigDecimal popularity
) {
}
