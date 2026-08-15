package pl.janda.onepiecetcg.pricing.application.service;

import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansionMatchType;
import pl.janda.onepiecetcg.pricing.application.model.PriceableSingle;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CardmarketExpansionMatcher {

    private static final BigDecimal EXACT_CONFIDENCE = new BigDecimal("1.000");

    private static void addReleaseName(
            Map<String, HashSet<String>> releaseIdsByNormalizedName,
            String releaseName,
            String releaseId
    ) {
        if (releaseName == null || releaseName.isBlank() || releaseId == null || releaseId.isBlank()) {
            return;
        }
        releaseIdsByNormalizedName.computeIfAbsent(normalize(releaseName), ignored -> new HashSet<>()).add(releaseId);
    }

    static String normalize(String value) {
        var decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    public void match(
            List<CardmarketExpansion> expansions,
            List<PriceableSingle> singles,
            LocalDateTime matchedAt
    ) {
        var releaseIdsByNormalizedName = new HashMap<String, HashSet<String>>();
        for (var single : singles) {
            addReleaseName(releaseIdsByNormalizedName, single.getReleaseName(), single.getReleaseId());
            addReleaseName(releaseIdsByNormalizedName, single.getSetName(), single.getReleaseId());
        }

        for (var expansion : expansions) {
            if (expansion.getReleaseId() != null || expansion.getExpansionSlug() == null) {
                continue;
            }
            var releaseIds = releaseIdsByNormalizedName.get(normalize(expansion.getExpansionSlug()));
            if (releaseIds == null || releaseIds.size() != 1) {
                continue;
            }
            expansion.setReleaseId(releaseIds.iterator().next());
            expansion.setMatchType(CardmarketExpansionMatchType.NORMALIZED_NAME);
            expansion.setConfidence(EXACT_CONFIDENCE);
            expansion.setLastResolvedAt(matchedAt);
        }
    }
}
