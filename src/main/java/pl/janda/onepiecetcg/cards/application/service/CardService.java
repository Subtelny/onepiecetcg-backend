package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.*;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.cards.application.repository.SetCardQueryRepository;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CardService implements CardCatalogUseCase {

    private static final Pattern VARIANT_INDEX_PATTERN = Pattern.compile("(?:0|[pr][1-9]\\d*)");

    private static final Comparator<SetCard> VARIANT_INDEX_ORDER = Comparator
            .comparingInt((SetCard card) -> variantKindRank(card.getVariantIndex()))
            .thenComparingInt(card -> variantNumber(card.getVariantIndex()));

    private final SetCardQueryRepository setCardRepository;

    private final CardFilterOptionService cardFilterOptionService;

    private final SemanticQueryParser semanticQueryParser;

    @Override
    public SetCard getCardById(String id) {
        return setCardRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + id));
    }

    private static String normalizeVariantIndex(String variantIndex) {
        var normalized = variantIndex == null || variantIndex.isBlank()
                ? SetCard.DEFAULT_VARIANT_INDEX
                : variantIndex.trim().toLowerCase(Locale.ROOT);
        if (!VARIANT_INDEX_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid variant index: " + variantIndex);
        }
        return normalized;
    }

    @Override
    public List<String> getAllCardCodes() {
        return setCardRepository.findAllCardCodes();
    }

    private static int variantKindRank(String variantIndex) {
        if (SetCard.DEFAULT_VARIANT_INDEX.equals(variantIndex)) {
            return 0;
        }
        if (variantIndex != null && variantIndex.startsWith("p")) {
            return 1;
        }
        if (variantIndex != null && variantIndex.startsWith("r")) {
            return 2;
        }
        return 3;
    }

    @Override
    public List<SetCard> getRepresentativeCardsByCardCodes(List<String> cardCodes) {
        if (cardCodes == null || cardCodes.isEmpty()) {
            return List.of();
        }
        return setCardRepository.findRepresentativesByCardSetIds(cardCodes);
    }

    @Override
    public List<SetCard> getCardsByVariantReferences(List<CardVariantReference> references) {
        if (references == null || references.isEmpty()) {
            return List.of();
        }
        var normalizedReferences = references.stream()
                .filter(Objects::nonNull)
                .map(reference -> new CardVariantReference(
                        reference.cardCode().trim(),
                        normalizeVariantIndex(reference.variantIndex())))
                .distinct()
                .toList();
        if (normalizedReferences.isEmpty()) {
            return List.of();
        }

        var requestedReferences = new HashSet<>(normalizedReferences);
        var cardCodes = normalizedReferences.stream()
                .map(CardVariantReference::cardCode)
                .distinct()
                .toList();
        return setCardRepository.findByCardSetIdIn(cardCodes).stream()
                .filter(card -> requestedReferences.contains(
                        new CardVariantReference(card.getCardSetId(), card.getVariantIndex())))
                .toList();
    }

    @Override
    public PagedCards searchCards(CardSearchQuery query) {
        var resolvedPage = query.page() != null ? query.page() : 0;
        var resolvedLimit = query.limit() != null ? query.limit() : 50;
        var resolvedSearchField = query.searchField() != null ? query.searchField() : CardSearchField.NAME;
        var resolvedShowAllVariants = Boolean.TRUE.equals(query.showAllVariants());

        var resolvedText = query.text();
        var resolvedCosts = query.costs();
        var resolvedPower = query.power();
        var resolvedCounterAmount = query.counterAmount();
        var resolvedErrataOnly = false;


        if (resolvedSearchField == CardSearchField.SEMANTIC && query.text() != null && !query.text().isBlank()) {
            var parsed = semanticQueryParser.parse(query.text());
            if (parsed.cost() != null && (query.costs() == null || query.costs().isEmpty())) {
                resolvedCosts = List.of(parsed.cost());
            }
            if (parsed.power() != null && query.power() == null) {
                resolvedPower = parsed.power();
            }
            if (parsed.counter() != null && query.counterAmount() == null) {
                resolvedCounterAmount = parsed.counter();
            }
            resolvedText = parsed.remainingText();
            resolvedErrataOnly = parsed.errataOnly();
        }

        var criteria = new CardSearchCriteria(
                resolvedText,
                resolvedSearchField,
                query.types(),
                query.colors(),
                query.rarities(),
                query.flatRarities(),
                resolvedCosts,
                resolvedPower,
                resolvedCounterAmount,
                query.attributes(),
                query.attributeCombos(),
                query.subTypes(),
                query.prefixes(),
                query.sortBy(),
                query.sortOrder(),
                resolvedPage,
                resolvedLimit,
                resolvedShowAllVariants,
                resolvedErrataOnly);

        var pageContent = setCardRepository.search(criteria);
        var totalCount = setCardRepository.countSearch(criteria);

        return new PagedCards(pageContent, totalCount, resolvedPage, resolvedLimit);
    }

    @Override
    public CardFilterOptions getFilterOptions() {
        return cardFilterOptionService.getFilterOptions();
    }

    private static int variantNumber(String variantIndex) {
        if (variantIndex == null || variantIndex.length() < 2) {
            return 0;
        }
        try {
            return Integer.parseInt(variantIndex.substring(1));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public List<SetCard> getVariantsByCardId(String id) {
        var card = getCardById(id);
        var cardSetId = card.getCardSetId();
        if (cardSetId == null) {
            return List.of(card);
        }
        return setCardRepository.findByCardSetId(cardSetId).stream()
                .sorted(VARIANT_INDEX_ORDER)
                .toList();
    }

    @Override
    public SetCard getVariantByCardCode(String cardCode, String variant) {
        var variantIndex = normalizeVariantIndex(variant);
        return setCardRepository.findByCardSetIdAndVariantIndex(cardCode, variantIndex)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Card not found with code: " + cardCode + ", variant: " + variantIndex));
    }
}
