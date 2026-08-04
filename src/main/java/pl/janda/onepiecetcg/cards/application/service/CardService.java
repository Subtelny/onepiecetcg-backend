package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.*;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.cards.application.repository.SetCardQueryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService implements CardCatalogUseCase {

    private final SetCardQueryRepository setCardRepository;

    private final CardFilterOptionService cardFilterOptionService;

    private final SemanticQueryParser semanticQueryParser;

    @Override
    public SetCard getCardById(String id) {
        return setCardRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + id));
    }

    @Override
    public List<SetCard> getVariantsByCardId(String id) {
        var card = getCardById(id);
        var cardSetId = card.getCardSetId();
        if (cardSetId == null) {
            return List.of(card);
        }
        return setCardRepository.findByCardSetId(cardSetId).stream()
                .sorted(CardRepresentativeService.CANONICAL_VARIANT_ORDER)
                .toList();
    }

    @Override
    public List<String> getAllCardCodes() {
        return setCardRepository.findAllCardCodes();
    }

    @Override
    public SetCard getVariantByCardCode(String cardCode, Integer variant) {
        var variants = setCardRepository.findByCardSetId(cardCode).stream()
                .sorted(CardRepresentativeService.CANONICAL_VARIANT_ORDER)
                .toList();
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("Card not found with code: " + cardCode);
        }
        var index = variant != null ? variant : 0;
        if (index < 0 || index >= variants.size()) {
            throw new IllegalArgumentException("Variant index out of range for card code: " + cardCode + ", variant: " + index);
        }
        return variants.get(index);
    }

    @Override
    public List<SetCard> getRepresentativeCardsByCardCodes(List<String> cardCodes) {
        if (cardCodes == null || cardCodes.isEmpty()) {
            return List.of();
        }
        return setCardRepository.findRepresentativesByCardSetIds(cardCodes);
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
}
