package pl.janda.onepiecetcg.matchups.application.model;

import java.math.BigDecimal;
import java.util.Map;

public record RepresentativeDeck(
        String leaderCode,
        Map<String, Integer> cards,
        long games,
        BigDecimal winRate
) {
}
