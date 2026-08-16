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
    private static final Pattern RELEASE_YEAR_PATTERN = Pattern.compile("(?<!\\d)(20\\d{2})(?!\\d)");
    private static final Pattern DATE_PATTERN = Pattern.compile("^(20\\d{2})-(\\d{2})-(\\d{2})");
    private static final BigDecimal EXACT_CONFIDENCE = new BigDecimal("1.000");
    private static final BigDecimal UNIQUE_CODE_CONFIDENCE = new BigDecimal("0.950");
    private static final BigDecimal HEURISTIC_CONFIDENCE = new BigDecimal("0.800");
    private static final BigDecimal DATED_RELEASE_HEURISTIC_CONFIDENCE = new BigDecimal("0.700");

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

    private static String releaseYear(String releaseName) {
        if (releaseName == null) {
            return null;
        }
        var matcher = RELEASE_YEAR_PATTERN.matcher(releaseName);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static ProductDate productDate(String dateAdded) {
        if (dateAdded == null) {
            return null;
        }
        var matcher = DATE_PATTERN.matcher(dateAdded.trim());
        return matcher.find()
                ? new ProductDate(matcher.group(1), matcher.group())
                : null;
    }

    public List<CardmarketSingleMapping> match(
            List<CardmarketPriceCandidate> candidates,
            List<PriceableSingle> singles,
            List<CardmarketExpansion> expansions,
            List<CardmarketSingleMapping> existingMappings,
            LocalDateTime matchedAt
    ) {
        var singleGroups = groupSinglesByMappedExpansion(singles, expansions);
        var candidateGroups = candidates.stream().collect(Collectors.groupingBy(this::candidateKey));
        var existingProductIds = existingMappings.stream()
                .map(CardmarketSingleMapping::getCardmarketProductId)
                .collect(Collectors.toSet());
        var usedPriceReferences = existingMappings.stream()
                .map(CardmarketSingleMapping::getPriceReference)
                .collect(Collectors.toSet());
        var matched = new ArrayList<CardmarketSingleMapping>();

        var datedReleaseMappings = matchDatedReleaseGroups(
                candidates, singles, existingProductIds, usedPriceReferences, matchedAt);
        datedReleaseMappings.forEach(mapping -> {
            existingProductIds.add(mapping.getCardmarketProductId());
            usedPriceReferences.add(mapping.getPriceReference());
            matched.add(mapping);
        });

        candidateGroups.forEach((key, products) -> {
            var matchingSingles = singleGroups.getOrDefault(key, List.of()).stream()
                    .sorted(singleComparator())
                    .toList();
            if (matchingSingles.isEmpty()) {
                return;
            }
            matchGroup(products, matchingSingles, existingProductIds, usedPriceReferences, matchedAt).stream()
                    .filter(mapping -> !existingProductIds.contains(mapping.getCardmarketProductId()))
                    .filter(mapping -> usedPriceReferences.add(mapping.getPriceReference()))
                    .forEach(mapping -> {
                        existingProductIds.add(mapping.getCardmarketProductId());
                        matched.add(mapping);
                    });
        });

        matchUniqueCardCodeGroups(
                candidates, singles, existingProductIds, usedPriceReferences, matchedAt).forEach(mapping -> {
            existingProductIds.add(mapping.getCardmarketProductId());
            usedPriceReferences.add(mapping.getPriceReference());
            matched.add(mapping);
        });
        return matched;
    }

    private List<CardmarketSingleMapping> matchUniqueCardCodeGroups(
            List<CardmarketPriceCandidate> candidates,
            List<PriceableSingle> singles,
            Set<Long> existingProductIds,
            Set<String> usedPriceReferences,
            LocalDateTime matchedAt
    ) {
        var singlesByCode = singles.stream()
                .filter(single -> single.getPriceReference() != null)
                .filter(single -> !usedPriceReferences.contains(single.getPriceReference()))
                .collect(Collectors.groupingBy(
                        single -> normalizeCardCode(single.getCardCode()),
                        TreeMap::new,
                        Collectors.toMap(
                                PriceableSingle::getPriceReference,
                                Function.identity(),
                                (first, second) -> first,
                                LinkedHashMap::new)));
        var productsByCode = candidates.stream()
                .filter(product -> product.getProductId() != null && product.getExpansionId() != null)
                .filter(product -> !existingProductIds.contains(product.getProductId()))
                .collect(Collectors.groupingBy(
                        product -> normalizeCardCode(product.getCardCode()),
                        Collectors.toMap(
                                CardmarketPriceCandidate::getProductId,
                                Function.identity(),
                                (first, second) -> first,
                                LinkedHashMap::new)));

        var result = new ArrayList<CardmarketSingleMapping>();
        singlesByCode.forEach((cardCode, singlesByReference) -> {
            var productsById = productsByCode.get(cardCode);
            if (singlesByReference.size() != 1 || productsById == null || productsById.size() != 1) {
                return;
            }
            result.add(toMapping(
                    productsById.values().iterator().next(),
                    singlesByReference.values().iterator().next(),
                    1,
                    CardmarketSingleMatchType.CODE_SINGLE_MATCH,
                    UNIQUE_CODE_CONFIDENCE,
                    matchedAt));
        });
        return result;
    }

    private List<CardmarketSingleMapping> matchDatedReleaseGroups(
            List<CardmarketPriceCandidate> candidates,
            List<PriceableSingle> singles,
            Set<Long> existingProductIds,
            Set<String> usedPriceReferences,
            LocalDateTime matchedAt
    ) {
        var singlesByCardAndYear = singles.stream()
                .filter(single -> !usedPriceReferences.contains(single.getPriceReference()))
                .filter(single -> releaseYear(single.getReleaseName()) != null)
                .collect(Collectors.groupingBy(single -> new DatedSingleKey(
                        normalizeCardCode(single.getCardCode()),
                        releaseYear(single.getReleaseName()))));

        var productsByCardAndDate = candidates.stream()
                .filter(product -> product.getProductId() != null && product.getExpansionId() != null)
                .filter(product -> !existingProductIds.contains(product.getProductId()))
                .filter(product -> productDate(product.getDateAdded()) != null)
                .collect(Collectors.groupingBy(product -> {
                    var date = productDate(product.getDateAdded());
                    return new DatedProductKey(
                            normalizeCardCode(product.getCardCode()),
                            date.year(),
                            date.date());
                }));

        var result = new ArrayList<CardmarketSingleMapping>();
        singlesByCardAndYear.forEach((singleKey, matchingSingles) -> {
            var candidateDays = productsByCardAndDate.entrySet().stream()
                    .filter(entry -> entry.getKey().cardCode().equals(singleKey.cardCode()))
                    .filter(entry -> entry.getKey().year().equals(singleKey.year()))
                    .map(Map.Entry::getValue)
                    .filter(products -> products.size() == matchingSingles.size())
                    .toList();
            if (candidateDays.size() != 1) {
                return;
            }

            var orderedSingles = matchingSingles.stream().sorted(singleComparator()).toList();
            var orderedProducts = candidateDays.getFirst().stream().sorted(productComparator()).toList();
            for (var i = 0; i < orderedSingles.size(); i++) {
                result.add(toMapping(
                        orderedProducts.get(i),
                        orderedSingles.get(i),
                        i + 1,
                        CardmarketSingleMatchType.CODE_EXPANSION_ORDER_HEURISTIC,
                        DATED_RELEASE_HEURISTIC_CONFIDENCE,
                        matchedAt));
            }
        });
        return result;
    }

    private List<CardmarketSingleMapping> matchGroup(
            List<CardmarketPriceCandidate> products,
            List<PriceableSingle> singles,
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
            var version = parseVersion(product.getProductName());
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

    private record DatedSingleKey(String cardCode, String year) {
    }

    private record DatedProductKey(String cardCode, String year, String date) {
    }

    private record ProductDate(String year, String date) {
    }
}
