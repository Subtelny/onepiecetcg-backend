package pl.janda.onepiecetcg.pricing.application.service;

import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.pricing.application.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CardmarketSingleMatcher {

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(?:^|[\\s(])V\\.?\\s*(\\d+)(?=[\\s)]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VARIANT_PATTERN = Pattern.compile("^([pr])(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final BigDecimal EXACT_CONFIDENCE = new BigDecimal("1.000");
    private static final BigDecimal HEURISTIC_CONFIDENCE = new BigDecimal("0.800");

    private static Comparator<PriceableSingle> singleComparator() {
        return Comparator
                .comparingInt((PriceableSingle single) -> variantTypeRank(single.getVariantIndex()))
                .thenComparingInt(single -> variantNumber(single.getVariantIndex()))
                .thenComparing(PriceableSingle::getSourceCardId, Comparator.nullsLast(String::compareTo));
    }

    private static int variantTypeRank(String variantIndex) {
        if (variantIndex == null || variantIndex.equals("0")) {
            return 0;
        }
        var matcher = VARIANT_PATTERN.matcher(variantIndex);
        if (!matcher.matches()) {
            return Integer.MAX_VALUE;
        }
        return matcher.group(1).equalsIgnoreCase("p") ? 1 : 2;
    }

    private static int variantNumber(String variantIndex) {
        if (variantIndex == null || variantIndex.equals("0")) {
            return 0;
        }
        var matcher = VARIANT_PATTERN.matcher(variantIndex);
        return matcher.matches() ? Integer.parseInt(matcher.group(2)) : Integer.MAX_VALUE;
    }

    private static Comparator<CardmarketPriceCandidate> productComparator() {
        return Comparator
                .comparing(CardmarketPriceCandidate::getDateAdded, Comparator.nullsLast(String::compareTo))
                .thenComparing(CardmarketPriceCandidate::getProductId);
    }

    private static Integer productVersion(
            CardmarketPriceCandidate product,
            Map<Long, CardmarketProductPage> pagesByProductId
    ) {
        var page = pagesByProductId.get(product.getProductId());
        return page != null && page.getVersion() != null
                ? page.getVersion()
                : parseVersion(product.getProductName());
    }

    static Integer parseVersion(String productName) {
        if (productName == null) {
            return null;
        }
        var matcher = VERSION_PATTERN.matcher(productName);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    private static CardmarketSingleMapping toMapping(
            CardmarketPriceCandidate product,
            PriceableSingle single,
            Integer localVariant,
            CardmarketSingleMatchType matchType,
            BigDecimal confidence,
            LocalDateTime matchedAt
    ) {
        return CardmarketSingleMapping.builder()
                .cardmarketProductId(product.getProductId())
                .priceReference(single.getPriceReference())
                .cardCode(product.getCardCode())
                .expansionId(product.getExpansionId())
                .localVariant(localVariant)
                .matchType(matchType)
                .confidence(confidence)
                .lastMatchedAt(matchedAt)
                .build();
    }

    private static String normalizeCardCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public List<CardmarketProductPageRequest> findVersionResolutionRequests(
            List<CardmarketPriceCandidate> candidates,
            List<PriceableSingle> singles,
            List<CardmarketExpansion> expansions,
            List<CardmarketSingleMapping> existingMappings
    ) {
        var candidateGroups = candidates.stream().collect(Collectors.groupingBy(this::candidateKey));
        var singleGroups = groupSinglesByMappedExpansion(singles, expansions);
        var existingProductIds = existingMappings.stream()
                .map(CardmarketSingleMapping::getCardmarketProductId)
                .collect(Collectors.toSet());
        var requests = new ArrayList<CardmarketProductPageRequest>();

        candidateGroups.forEach((key, products) -> {
            var matchingSingles = singleGroups.getOrDefault(key, List.of());
            if (products.size() <= 1 || products.size() != matchingSingles.size()) {
                return;
            }
            products.stream()
                    .filter(product -> !existingProductIds.contains(product.getProductId()))
                    .filter(product -> parseVersion(product.getProductName()) == null)
                    .map(product -> CardmarketProductPageRequest.builder()
                            .productId(product.getProductId())
                            .expansionId(product.getExpansionId())
                            .build())
                    .forEach(requests::add);
        });
        return requests;
    }

    public List<CardmarketSingleMapping> match(
            List<CardmarketPriceCandidate> candidates,
            List<PriceableSingle> singles,
            List<CardmarketExpansion> expansions,
            List<CardmarketProductPage> productPages,
            List<CardmarketSingleMapping> existingMappings,
            LocalDateTime matchedAt
    ) {
        var singleGroups = groupSinglesByMappedExpansion(singles, expansions);
        var candidateGroups = candidates.stream().collect(Collectors.groupingBy(this::candidateKey));
        var pagesByProductId = productPages.stream().collect(Collectors.toMap(
                CardmarketProductPage::getProductId, Function.identity(), (first, second) -> second));
        var existingProductIds = existingMappings.stream()
                .map(CardmarketSingleMapping::getCardmarketProductId)
                .collect(Collectors.toSet());
        var usedPriceReferences = existingMappings.stream()
                .map(CardmarketSingleMapping::getPriceReference)
                .collect(Collectors.toSet());
        var matched = new ArrayList<CardmarketSingleMapping>();

        candidateGroups.forEach((key, products) -> {
            var matchingSingles = singleGroups.getOrDefault(key, List.of()).stream()
                    .sorted(singleComparator())
                    .toList();
            if (matchingSingles.isEmpty()) {
                return;
            }
            matchGroup(products, matchingSingles, pagesByProductId, existingProductIds,
                    usedPriceReferences, matchedAt).stream()
                    .filter(mapping -> !existingProductIds.contains(mapping.getCardmarketProductId()))
                    .filter(mapping -> usedPriceReferences.add(mapping.getPriceReference()))
                    .forEach(matched::add);
        });
        return matched;
    }

    private List<CardmarketSingleMapping> matchGroup(
            List<CardmarketPriceCandidate> products,
            List<PriceableSingle> singles,
            Map<Long, CardmarketProductPage> pagesByProductId,
            Set<Long> existingProductIds,
            Set<String> usedPriceReferences,
            LocalDateTime matchedAt
    ) {
        if (products.size() == 1 && singles.size() == 1) {
            return List.of(toMapping(products.getFirst(), singles.getFirst(), 1,
                    CardmarketSingleMatchType.CODE_EXPANSION_SINGLE_MATCH, EXACT_CONFIDENCE, matchedAt));
        }

        var availableProducts = products.stream()
                .filter(product -> !existingProductIds.contains(product.getProductId()))
                .toList();
        var availableSingles = singles.stream()
                .filter(single -> !usedPriceReferences.contains(single.getPriceReference()))
                .toList();
        var localVariantByReference = new HashMap<String, Integer>();
        for (var i = 0; i < singles.size(); i++) {
            localVariantByReference.put(singles.get(i).getPriceReference(), i + 1);
        }

        var result = new ArrayList<CardmarketSingleMapping>();
        var matchedProductIds = new HashSet<Long>();
        var matchedReferences = new HashSet<String>();
        for (var product : availableProducts) {
            var version = productVersion(product, pagesByProductId);
            if (version == null || version < 1 || version > singles.size()) {
                continue;
            }
            var single = singles.get(version - 1);
            if (usedPriceReferences.contains(single.getPriceReference()) || !matchedReferences.add(single.getPriceReference())) {
                continue;
            }
            result.add(toMapping(product, single, version,
                    CardmarketSingleMatchType.CODE_EXPANSION_VERSION, EXACT_CONFIDENCE, matchedAt));
            matchedProductIds.add(product.getProductId());
        }

        var remainingProducts = availableProducts.stream()
                .filter(product -> !matchedProductIds.contains(product.getProductId()))
                .sorted(productComparator())
                .toList();
        var remainingSingles = availableSingles.stream()
                .filter(single -> !matchedReferences.contains(single.getPriceReference()))
                .toList();
        if (remainingProducts.size() != remainingSingles.size()) {
            return result;
        }

        var complementOfVersions = !result.isEmpty() && remainingProducts.size() == 1;
        for (var i = 0; i < remainingProducts.size(); i++) {
            var single = remainingSingles.get(i);
            var localVariant = localVariantByReference.get(single.getPriceReference());
            result.add(toMapping(
                    remainingProducts.get(i),
                    single,
                    localVariant,
                    complementOfVersions
                            ? CardmarketSingleMatchType.CODE_EXPANSION_VERSION
                            : CardmarketSingleMatchType.CODE_EXPANSION_ORDER_HEURISTIC,
                    complementOfVersions ? EXACT_CONFIDENCE : HEURISTIC_CONFIDENCE,
                    matchedAt));
        }
        return result;
    }

    private Map<GroupKey, List<PriceableSingle>> groupSinglesByMappedExpansion(
            List<PriceableSingle> singles,
            List<CardmarketExpansion> expansions
    ) {
        var expansionIdsByReleaseId = expansions.stream()
                .filter(expansion -> expansion.getReleaseId() != null)
                .collect(Collectors.groupingBy(
                        CardmarketExpansion::getReleaseId,
                        Collectors.mapping(CardmarketExpansion::getExpansionId, Collectors.toList())));
        var groups = new HashMap<GroupKey, List<PriceableSingle>>();
        for (var single : singles) {
            for (var expansionId : expansionIdsByReleaseId.getOrDefault(single.getReleaseId(), List.of())) {
                var key = new GroupKey(normalizeCardCode(single.getCardCode()), expansionId);
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(single);
            }
        }
        return groups;
    }

    private GroupKey candidateKey(CardmarketPriceCandidate candidate) {
        return new GroupKey(normalizeCardCode(candidate.getCardCode()), candidate.getExpansionId());
    }

    private record GroupKey(String cardCode, Long expansionId) {
    }
}
