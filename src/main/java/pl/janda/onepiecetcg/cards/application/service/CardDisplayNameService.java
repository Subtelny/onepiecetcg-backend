package pl.janda.onepiecetcg.cards.application.service;

import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class CardDisplayNameService {

    private static final Pattern WINNER_PRODUCT = Pattern.compile(
            "(?i)(?<![\\p{L}\\p{N}_])winner(?![\\p{L}\\p{N}_])");

    private static final Pattern CARD_SET_PRODUCT = Pattern.compile(
            "(?i)\\[[a-z0-9]+(?:-[a-z0-9]+)*]$");

    private static String buildDisplayName(
            SetCard card,
            long winnerCount,
            Map<String, Integer> productCounts,
            Map<SetProductVariantKind, Long> setProductVariantCounts
    ) {
        var cardName = blankToNull(card.getCardName());
        if (card.isRepresentative() || cardName == null) {
            return cardName;
        }

        var sourceProduct = blankToNull(card.getSourceProduct());
        if (sourceProduct == null) {
            return appendVariantIndex(cardName, card.getVariantIndex());
        }

        if (isWinnerProduct(sourceProduct)) {
            var productLabel = winnerCount == 1 ? "Winner" : sourceProduct;
            var displayName = cardName + " (" + productLabel + ")";
            var productKey = normalizeProductKey(sourceProduct);
            return productCounts.getOrDefault(productKey, 0) > 1
                    ? appendVariantIndex(displayName, card.getVariantIndex())
                    : displayName;
        }

        var setProductVariantKind = setProductVariantKind(card);
        if (isCardSetProduct(sourceProduct) && setProductVariantKind != null) {
            var displayName = cardName + " (" + setProductVariantKind.label + ")";
            return setProductVariantCounts.getOrDefault(setProductVariantKind, 0L) > 1
                    ? appendVariantIndex(displayName, card.getVariantIndex())
                    : displayName;
        }

        var productLabel = sourceProduct;
        var displayName = cardName + " (" + productLabel + ")";
        var productKey = normalizeProductKey(sourceProduct);
        return productCounts.getOrDefault(productKey, 0) > 1
                ? appendVariantIndex(displayName, card.getVariantIndex())
                : displayName;
    }

    private static String appendVariantIndex(String displayName, String variantIndex) {
        var normalizedIndex = blankToNull(variantIndex);
        return normalizedIndex == null ? displayName : displayName + " [" + normalizedIndex + "]";
    }

    private static boolean isWinnerProduct(String sourceProduct) {
        var normalized = blankToNull(sourceProduct);
        return normalized != null && WINNER_PRODUCT.matcher(normalized).find();
    }

    private static boolean isCardSetProduct(String sourceProduct) {
        var normalized = blankToNull(sourceProduct);
        return normalized != null && CARD_SET_PRODUCT.matcher(normalized).find();
    }

    private static SetProductVariantKind setProductVariantKind(SetCard card) {
        var variantIndex = blankToNull(card.getVariantIndex());
        if (variantIndex == null) {
            return null;
        }
        if (variantIndex.matches("(?i)p[1-9]\\d*")) {
            return SetProductVariantKind.ALT;
        }
        if (variantIndex.matches("(?i)r[1-9]\\d*")) {
            return SetProductVariantKind.REPRINT;
        }
        return null;
    }

    private static String normalizeProductKey(String sourceProduct) {
        var normalized = blankToNull(sourceProduct);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public void assignDisplayNames(List<SetCard> cards) {
        var cardsByCode = new HashMap<String, List<SetCard>>();
        for (var card : cards) {
            cardsByCode.computeIfAbsent(card.getCardSetId(), ignored -> new ArrayList<>()).add(card);
        }
        cardsByCode.values().forEach(this::assignDisplayNamesForCard);
    }

    private void assignDisplayNamesForCard(List<SetCard> variants) {
        var nonDefaultVariants = variants.stream()
                .filter(card -> !card.isRepresentative())
                .toList();
        var winnerCount = nonDefaultVariants.stream()
                .map(SetCard::getSourceProduct)
                .filter(CardDisplayNameService::isWinnerProduct)
                .count();
        var productCounts = new HashMap<String, Integer>();
        nonDefaultVariants.stream()
                .map(SetCard::getSourceProduct)
                .map(CardDisplayNameService::normalizeProductKey)
                .filter(product -> product != null)
                .forEach(product -> productCounts.merge(product, 1, Integer::sum));
        var setProductVariantCounts = new EnumMap<SetProductVariantKind, Long>(SetProductVariantKind.class);
        nonDefaultVariants.stream()
                .filter(card -> !isWinnerProduct(card.getSourceProduct()))
                .filter(card -> isCardSetProduct(card.getSourceProduct()))
                .map(CardDisplayNameService::setProductVariantKind)
                .filter(Objects::nonNull)
                .forEach(kind -> setProductVariantCounts.merge(kind, 1L, Long::sum));

        for (var card : variants) {
            card.setDisplayName(buildDisplayName(card, winnerCount, productCounts, setProductVariantCounts));
        }
    }

    private enum SetProductVariantKind {
        ALT("Alt"),
        REPRINT("Reprint");

        private final String label;

        SetProductVariantKind(String label) {
            this.label = label;
        }
    }
}
