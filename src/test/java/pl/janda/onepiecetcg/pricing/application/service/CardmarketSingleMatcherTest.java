package pl.janda.onepiecetcg.pricing.application.service;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.pricing.application.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardmarketSingleMatcherTest {

    private final CardmarketSingleMatcher matcher = new CardmarketSingleMatcher();

    private static PriceableSingle single(
            String priceReference,
            String sourceCardId,
            String releaseId,
            String variantIndex
    ) {
        return PriceableSingle.builder()
                .priceReference(priceReference)
                .sourceCardId(sourceCardId)
                .cardCode("EB01-001")
                .releaseId(releaseId)
                .variantIndex(variantIndex)
                .build();
    }

    private static CardmarketExpansion expansion(Long expansionId, String releaseId) {
        return CardmarketExpansion.builder()
                .expansionId(expansionId)
                .releaseId(releaseId)
                .lastResolvedAt(LocalDateTime.now())
                .build();
    }

    private static CardmarketPriceCandidate product(Long productId, Long expansionId, String dateAdded) {
        return CardmarketPriceCandidate.builder()
                .productId(productId)
                .cardCode("EB01-001")
                .expansionId(expansionId)
                .productName("Kouzuki Oden (EB01-001)")
                .dateAdded(dateAdded)
                .build();
    }

    private static CardmarketProductPage page(Long productId, Long expansionId, Integer version) {
        return CardmarketProductPage.builder()
                .productId(productId)
                .expansionId(expansionId)
                .version(version)
                .build();
    }

    @Test
    void match_usesReleaseAndLocalVariantInsteadOfTheGlobalParallelSuffix() {
        var singles = List.of(
                single("single:EB01-001", "EB01-001", "569201", "0"),
                single("single:EB01-001_p1", "EB01-001_p1", "569201", "p1"),
                single("single:EB01-001_p2", "EB01-001_p2", "569202", "p2"));
        var expansions = List.of(
                expansion(5585L, "569201"),
                expansion(6028L, "569202"));
        var products = List.of(
                product(767953L, 5585L, "2024-05-02 08:45:36"),
                product(767954L, 5585L, "2024-05-02 08:45:55"),
                product(823420L, 6028L, "2025-07-01 08:00:00"));
        var pages = List.of(
                page(767953L, 5585L, 1),
                page(767954L, 5585L, 2),
                page(823420L, 6028L, null));

        var result = matcher.match(products, singles, expansions, pages, List.of(), LocalDateTime.now());

        assertThat(result).extracting(mapping -> mapping.getCardmarketProductId() + "=" + mapping.getPriceReference())
                .containsExactlyInAnyOrder(
                        "767953=single:EB01-001",
                        "767954=single:EB01-001_p1",
                        "823420=single:EB01-001_p2");
        assertThat(result).filteredOn(mapping -> mapping.getCardmarketProductId().equals(767954L)).singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.getLocalVariant()).isEqualTo(2);
                    assertThat(mapping.getMatchType()).isEqualTo(CardmarketSingleMatchType.CODE_EXPANSION_VERSION);
                    assertThat(mapping.getConfidence()).isEqualByComparingTo("1.000");
                });
        assertThat(result).filteredOn(mapping -> mapping.getCardmarketProductId().equals(823420L)).singleElement()
                .satisfies(mapping -> {
                    assertThat(mapping.getLocalVariant()).isEqualTo(1);
                    assertThat(mapping.getMatchType()).isEqualTo(
                            CardmarketSingleMatchType.CODE_EXPANSION_SINGLE_MATCH);
                });
    }

    @Test
    void match_marksDateAndProductOrderingAsAHeuristicFallback() {
        var singles = List.of(
                single("single:EB01-001", "EB01-001", "569201", "0"),
                single("single:EB01-001_p1", "EB01-001_p1", "569201", "p1"));
        var products = List.of(
                product(767954L, 5585L, "2024-05-02 08:45:55"),
                product(767953L, 5585L, "2024-05-02 08:45:36"));

        var result = matcher.match(
                products, singles, List.of(expansion(5585L, "569201")), List.of(), List.of(), LocalDateTime.now());

        assertThat(result).allSatisfy(mapping -> {
            assertThat(mapping.getMatchType()).isEqualTo(
                    CardmarketSingleMatchType.CODE_EXPANSION_ORDER_HEURISTIC);
            assertThat(mapping.getConfidence()).isEqualByComparingTo(new BigDecimal("0.800"));
        });
        assertThat(result).filteredOn(mapping -> mapping.getCardmarketProductId().equals(767953L)).singleElement()
                .extracting("priceReference").isEqualTo("single:EB01-001");
    }
}
