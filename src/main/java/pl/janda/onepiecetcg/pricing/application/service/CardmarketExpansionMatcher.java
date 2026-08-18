package pl.janda.onepiecetcg.pricing.application.service;

import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansionMatchType;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.pricing.application.model.PriceableSingle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Maps a Cardmarket expansion onto a catalog release by how much of the expansion's card codes that
 * release actually prints. Names can't do this job: catalog release names carry the set code and a
 * product-type prefix ("BOOSTER PACK -ROMANCE DAWN- [OP-01]") that Cardmarket's ("Romance Dawn") never
 * has, so exact-name matching resolved almost nothing. Card codes are the one identifier both sides
 * spell identically.
 */
@Service
public class CardmarketExpansionMatcher {

    /**
     * Share of an expansion's card codes that a release must print before it is accepted. Cardmarket
     * expansions are usually a strict subset of a release (they drop unpriced cards), so a genuine
     * match sits near 1.0 while cross-set promo/tournament expansions stay far below.
     */
    private static final double MIN_CONTAINMENT = 0.90;

    private static String normalizeCardCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<Long, Set<String>> cardCodesByExpansion(List<CardmarketPriceCandidate> candidates) {
        return candidates.stream()
                .filter(candidate -> candidate.getExpansionId() != null)
                .filter(candidate -> !normalizeCardCode(candidate.getCardCode()).isEmpty())
                .collect(Collectors.groupingBy(
                        CardmarketPriceCandidate::getExpansionId,
                        Collectors.mapping(
                                candidate -> normalizeCardCode(candidate.getCardCode()),
                                Collectors.toSet())));
    }

    private static Map<String, Set<String>> cardCodesByRelease(List<PriceableSingle> singles) {
        return singles.stream()
                .filter(single -> single.getReleaseId() != null && !single.getReleaseId().isBlank())
                .filter(single -> !normalizeCardCode(single.getCardCode()).isEmpty())
                .collect(Collectors.groupingBy(
                        PriceableSingle::getReleaseId,
                        Collectors.mapping(
                                single -> normalizeCardCode(single.getCardCode()),
                                Collectors.toSet())));
    }

    private static int overlap(Set<String> expansionCodes, Set<String> releaseCodes) {
        var shared = 0;
        for (var code : expansionCodes) {
            if (releaseCodes.contains(code)) {
                shared++;
            }
        }
        return shared;
    }

    /**
     * Returns the single release containing at least {@link #MIN_CONTAINMENT} of the expansion's codes,
     * or empty when nothing clears the bar or two releases tie for best - an ambiguous expansion is left
     * unmapped rather than guessed, since a wrong release silently mismaps every card in it.
     */
    private static Optional<ReleaseMatch> bestRelease(
            Set<String> expansionCodes,
            Map<String, Set<String>> codesByRelease
    ) {
        String bestReleaseId = null;
        var bestContainment = 0.0;
        var tied = false;
        for (var release : codesByRelease.entrySet()) {
            var containment = (double) overlap(expansionCodes, release.getValue()) / expansionCodes.size();
            if (bestReleaseId == null || containment > bestContainment) {
                bestReleaseId = release.getKey();
                bestContainment = containment;
                tied = false;
            } else if (containment == bestContainment) {
                tied = true;
            }
        }
        return tied || bestContainment < MIN_CONTAINMENT
                ? Optional.empty()
                : Optional.of(new ReleaseMatch(bestReleaseId, bestContainment));
    }

    /**
     * Recomputes every mapping on each run instead of skipping already-resolved expansions: the score
     * depends on the catalog, which grows, so an expansion that could not be placed yesterday (or was
     * placed on a then-incomplete release) is re-evaluated for free against today's card codes.
     *
     * <p>Excluded expansions still get a row, only an unmapped one. Scoring them would succeed - a Japanese
     * print run contains exactly the release's card codes - so the exclusion has to be applied here rather
     * than left to the containment threshold.
     */
    public List<CardmarketExpansion> match(
            List<CardmarketExpansion> knownExpansions,
            List<CardmarketPriceCandidate> candidates,
            List<PriceableSingle> singles,
            List<Long> excludedExpansionIds,
            LocalDateTime matchedAt
    ) {
        var expansionsById = knownExpansions.stream().collect(Collectors.toMap(
                CardmarketExpansion::getExpansionId, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        var codesByRelease = cardCodesByRelease(singles);
        var excluded = Set.copyOf(excludedExpansionIds);

        cardCodesByExpansion(candidates).forEach((expansionId, expansionCodes) -> {
            var expansion = expansionsById.computeIfAbsent(expansionId, id ->
                    CardmarketExpansion.builder().expansionId(id).build());
            var match = excluded.contains(expansionId)
                    ? Optional.<ReleaseMatch>empty()
                    : bestRelease(expansionCodes, codesByRelease);
            expansion.setReleaseId(match.map(ReleaseMatch::releaseId).orElse(null));
            expansion.setMatchType(match.isPresent() ? CardmarketExpansionMatchType.CARD_CODE_OVERLAP : null);
            expansion.setConfidence(match.map(ReleaseMatch::confidence).orElse(null));
            expansion.setLastResolvedAt(matchedAt);
        });
        return List.copyOf(expansionsById.values());
    }

    private record ReleaseMatch(String releaseId, double containment) {

        BigDecimal confidence() {
            return BigDecimal.valueOf(containment).setScale(3, RoundingMode.HALF_UP);
        }
    }
}
