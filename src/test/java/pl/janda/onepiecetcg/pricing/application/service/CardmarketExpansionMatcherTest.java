package pl.janda.onepiecetcg.pricing.application.service;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansionMatchType;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.pricing.application.model.PriceableSingle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CardmarketExpansionMatcherTest {

    private final CardmarketExpansionMatcher matcher = new CardmarketExpansionMatcher();

    private static List<String> codes(String prefix, int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> "%s-%03d".formatted(prefix, i)).toList();
    }

    private static List<CardmarketPriceCandidate> products(Long expansionId, List<String> cardCodes) {
        return cardCodes.stream()
                .map(cardCode -> CardmarketPriceCandidate.builder()
                        .productId((long) cardCodes.indexOf(cardCode) + expansionId)
                        .expansionId(expansionId)
                        .cardCode(cardCode)
                        .productName("Card (" + cardCode + ")")
                        .build())
                .toList();
    }

    private static List<PriceableSingle> singles(String releaseId, List<String> cardCodes) {
        return cardCodes.stream()
                .map(cardCode -> PriceableSingle.builder()
                        .priceReference("single:" + releaseId + ":" + cardCode)
                        .cardCode(cardCode)
                        .releaseId(releaseId)
                        .build())
                .toList();
    }

    @SafeVarargs
    private static <T> List<T> concat(List<T>... lists) {
        return Stream.of(lists).flatMap(List::stream).toList();
    }

    @Test
    void match_mapsAnExpansionToTheReleasePrintingItsCardCodes() {
        var result = matcher.match(
                List.of(),
                products(5229L, codes("OP01", 20)),
                concat(singles("OP-01", codes("OP01", 20)), singles("OP-02", codes("OP02", 20))),
                List.of(),
                LocalDateTime.now());

        assertThat(result).singleElement().satisfies(expansion -> {
            assertThat(expansion.getExpansionId()).isEqualTo(5229L);
            assertThat(expansion.getReleaseId()).isEqualTo("OP-01");
            assertThat(expansion.getMatchType()).isEqualTo(CardmarketExpansionMatchType.CARD_CODE_OVERLAP);
            assertThat(expansion.getConfidence()).isEqualByComparingTo("1.000");
            assertThat(expansion.getLastResolvedAt()).isNotNull();
        });
    }

    @Test
    void match_toleratesAnExpansionMissingAFewOfTheReleasesCards() {
        var result = matcher.match(
                List.of(),
                products(5229L, codes("OP01", 19)),
                singles("OP-01", codes("OP01", 20)),
                List.of(),
                LocalDateTime.now());

        assertThat(result).singleElement()
                .satisfies(expansion -> assertThat(expansion.getReleaseId()).isEqualTo("OP-01"));
    }

    @Test
    void match_leavesACrossReleasePromoExpansionUnmapped() {
        var result = matcher.match(
                List.of(),
                concat(products(5230L, codes("OP01", 10)), products(5230L, codes("OP02", 10))),
                concat(singles("OP-01", codes("OP01", 10)), singles("OP-02", codes("OP02", 10))),
                List.of(),
                LocalDateTime.now());

        assertThat(result).singleElement().satisfies(expansion -> {
            assertThat(expansion.getReleaseId()).isNull();
            assertThat(expansion.getMatchType()).isNull();
            assertThat(expansion.getConfidence()).isNull();
        });
    }

    @Test
    void match_leavesAnExpansionUnmappedWhenTwoReleasesTieForBest() {
        var result = matcher.match(
                List.of(),
                products(5229L, codes("OP01", 10)),
                concat(singles("OP-01", codes("OP01", 10)), singles("OP-01-REPRINT", codes("OP01", 10))),
                List.of(),
                LocalDateTime.now());

        assertThat(result).singleElement()
                .satisfies(expansion -> assertThat(expansion.getReleaseId()).isNull());
    }

    @Test
    void match_clearsAStoredMappingThatNoLongerHoldsAgainstTheCurrentCatalog() {
        var stored = CardmarketExpansion.builder()
                .expansionId(5229L)
                .releaseId("OP-01")
                .matchType(CardmarketExpansionMatchType.CARD_CODE_OVERLAP)
                .lastResolvedAt(LocalDateTime.now().minusDays(1))
                .build();

        var result = matcher.match(
                List.of(stored),
                products(5229L, codes("OP01", 10)),
                singles("OP-02", codes("OP02", 10)),
                List.of(),
                LocalDateTime.now());

        assertThat(result).containsExactly(stored);
        assertThat(stored.getReleaseId()).isNull();
    }

    @Test
    void match_leavesAnExcludedPrintRunUnmappedEvenThoughItsCodesMatchPerfectly() {
        var result = matcher.match(
                List.of(),
                concat(products(5580L, codes("EB01", 20)), products(5585L, codes("EB01", 20))),
                singles("EB-01", codes("EB01", 20)),
                List.of(5580L),
                LocalDateTime.now());

        assertThat(result).filteredOn(expansion -> expansion.getExpansionId().equals(5580L)).singleElement()
                .satisfies(expansion -> {
                    assertThat(expansion.getReleaseId()).isNull();
                    assertThat(expansion.getMatchType()).isNull();
                    assertThat(expansion.getConfidence()).isNull();
                    assertThat(expansion.getLastResolvedAt()).isNotNull();
                });
        assertThat(result).filteredOn(expansion -> expansion.getExpansionId().equals(5585L)).singleElement()
                .satisfies(expansion -> assertThat(expansion.getReleaseId()).isEqualTo("EB-01"));
    }
}
