package pl.janda.onepiecetcg.pricing.application.service;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansionMatchType;
import pl.janda.onepiecetcg.pricing.application.model.PriceableSingle;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardmarketExpansionMatcherTest {

    private static CardmarketExpansion expansion(Long id, String slug) {
        return CardmarketExpansion.builder()
                .expansionId(id)
                .expansionSlug(slug)
                .lastResolvedAt(LocalDateTime.now())
                .build();
    }

    private static PriceableSingle single(String releaseId, String releaseName) {
        return PriceableSingle.builder()
                .priceReference("single:" + releaseId)
                .cardCode("EB01-001")
                .releaseId(releaseId)
                .releaseName(releaseName)
                .build();
    }

    @Test
    void match_resolvesCanonicalExpansionSlugsToCatalogReleaseIds() {
        var memorial = expansion(5585L, "Memorial-Collection");
        var anime = expansion(6028L, "Anime-25th-Collection");
        var singles = List.of(
                single("569201", "Memorial Collection"),
                single("569202", "Anime 25th Collection"));

        new CardmarketExpansionMatcher().match(List.of(memorial, anime), singles, LocalDateTime.now());

        assertThat(memorial.getReleaseId()).isEqualTo("569201");
        assertThat(anime.getReleaseId()).isEqualTo("569202");
        assertThat(memorial.getMatchType()).isEqualTo(CardmarketExpansionMatchType.NORMALIZED_NAME);
        assertThat(memorial.getConfidence()).isEqualByComparingTo("1.000");
    }
}
