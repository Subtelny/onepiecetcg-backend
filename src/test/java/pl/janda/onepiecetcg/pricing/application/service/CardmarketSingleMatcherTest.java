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

    private static PriceableSingle datedSingle(
            String priceReference,
            String sourceCardId,
            String releaseName,
            String variantIndex
    ) {
        return PriceableSingle.builder()
                .priceReference(priceReference)
                .sourceCardId(sourceCardId)
                .cardCode("EB01-003")
                .releaseId("569901")
                .releaseName(releaseName)
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

    private static CardmarketPriceCandidate versionedProduct(Long productId, Long expansionId, String productName) {
        return CardmarketPriceCandidate.builder()
                .productId(productId)
                .cardCode("EB01-001")
                .expansionId(expansionId)
                .productName(productName)
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
                versionedProduct(767953L, 5585L, "Kouzuki Oden (EB01-001) V.1"),
                versionedProduct(767954L, 5585L, "Kouzuki Oden (EB01-001) V.2"),
                product(823420L, 6028L, "2025-07-01 08:00:00"));

        var result = matcher.match(products, singles, expansions, List.of(), LocalDateTime.now());

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
                products, singles, List.of(expansion(5585L, "569201")), List.of(), LocalDateTime.now());

        assertThat(result).allSatisfy(mapping -> {
            assertThat(mapping.getMatchType()).isEqualTo(
                    CardmarketSingleMatchType.CODE_EXPANSION_ORDER_HEURISTIC);
            assertThat(mapping.getConfidence()).isEqualByComparingTo(new BigDecimal("0.800"));
        });
        assertThat(result).filteredOn(mapping -> mapping.getCardmarketProductId().equals(767953L)).singleElement()
                .extracting("priceReference").isEqualTo("single:EB01-001");
    }

    @Test
    void match_mapsAnUnambiguousDatedReleaseBatchWithoutExpansionPages() {
        var singles = List.of(
                datedSingle("single:EB01-003_p2", "EB01-003_p2",
                        "Offline Regional Participation Pack 2025 Vol.2", "p2"),
                datedSingle("single:EB01-003_p3", "EB01-003_p3",
                        "Offline Regional Finalist Card Set 2025 Vol.2", "p3"),
                datedSingle("single:EB01-003_p4", "EB01-003_p4",
                        "Offline Regional Champion Card Set 2025 Vol.2", "p4"));
        var products = List.of(
                CardmarketPriceCandidate.builder().productId(840680L).expansionId(5303L)
                        .cardCode("EB01-003").productName("Kid & Killer (EB01-003)")
                        .dateAdded("2025-08-04 09:45:16").build(),
                CardmarketPriceCandidate.builder().productId(840681L).expansionId(5262L)
                        .cardCode("EB01-003").productName("Kid & Killer (EB01-003)")
                        .dateAdded("2025-08-04 09:45:31").build(),
                CardmarketPriceCandidate.builder().productId(840688L).expansionId(5262L)
                        .cardCode("EB01-003").productName("Kid & Killer (EB01-003)")
                        .dateAdded("2025-08-04 09:51:13").build());

        var result = matcher.match(products, singles, List.of(), List.of(), LocalDateTime.now());

        assertThat(result).extracting(mapping -> mapping.getCardmarketProductId() + "=" + mapping.getPriceReference())
                .containsExactly(
                        "840680=single:EB01-003_p2",
                        "840681=single:EB01-003_p3",
                        "840688=single:EB01-003_p4");
        assertThat(result).allSatisfy(mapping -> {
            assertThat(mapping.getMatchType()).isEqualTo(
                    CardmarketSingleMatchType.CODE_EXPANSION_ORDER_HEURISTIC);
            assertThat(mapping.getConfidence()).isEqualByComparingTo("0.700");
        });
    }

    @Test
    void match_doesNotGuessWhenMultipleDatedBatchesHaveTheSameSize() {
        var singles = List.of(datedSingle(
                "single:EB01-003_p3", "EB01-003_p3", "Regional Finalist 2025", "p3"));
        var products = List.of(
                CardmarketPriceCandidate.builder().productId(1L).expansionId(1L)
                        .cardCode("EB01-003").productName("Kid & Killer (EB01-003)")
                        .dateAdded("2025-01-01 10:00:00").build(),
                CardmarketPriceCandidate.builder().productId(2L).expansionId(2L)
                        .cardCode("EB01-003").productName("Kid & Killer (EB01-003)")
                        .dateAdded("2025-02-01 10:00:00").build());

        assertThat(matcher.match(products, singles, List.of(), List.of(), LocalDateTime.now()))
                .isEmpty();
    }

    @Test
    void match_mapsGloballyUniqueCodeWithoutAnExpansionMapping() {
        var singles = List.of(single("single:EB01-001", "EB01-001", "EB-01", "0"));
        var products = List.of(product(767953L, 5585L, "2024-05-02 08:45:36"));

        var result = matcher.match(products, singles, List.of(), List.of(), LocalDateTime.now());

        assertThat(result).singleElement().satisfies(mapping -> {
            assertThat(mapping.getCardmarketProductId()).isEqualTo(767953L);
            assertThat(mapping.getPriceReference()).isEqualTo("single:EB01-001");
            assertThat(mapping.getMatchType()).isEqualTo(CardmarketSingleMatchType.CODE_SINGLE_MATCH);
            assertThat(mapping.getConfidence()).isEqualByComparingTo("0.950");
        });
    }

    @Test
    void match_doesNotGuessWhenAUniqueLocalCodeHasMultipleProducts() {
        var singles = List.of(single("single:EB01-001", "EB01-001", "EB-01", "0"));
        var products = List.of(
                product(767953L, 5585L, "2024-05-02 08:45:36"),
                product(823420L, 6028L, "2025-07-01 08:00:00"));

        assertThat(matcher.match(products, singles, List.of(), List.of(), LocalDateTime.now()))
                .isEmpty();
    }
}
