package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.CardColor;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.cards.application.model.CardRarity;
import pl.janda.onepiecetcg.cards.application.model.CardSearchField;
import pl.janda.onepiecetcg.cards.application.model.CardSortField;
import pl.janda.onepiecetcg.cards.application.model.CardType;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.model.SortDirection;
import pl.janda.onepiecetcg.cards.application.repository.SetCardRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final SetCardRepository setCardRepository;

    private final CardFilterOptionService cardFilterOptionService;

    private final SemanticQueryParser semanticQueryParser;

    public SetCard getCardById(String id) {
        return setCardRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + id));
    }

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

    public List<String> getAllCardCodes() {
        return setCardRepository.findAllCardCodes();
    }

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

    public PagedCards searchCards(
            String name,
            CardSearchField searchField,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            List<Integer> costs,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            CardSortField sortBy,
            SortDirection sortOrder,
            Integer page,
            Integer limit,
            Boolean showAllVariants
    ) {
        var resolvedPage = page != null ? page : 0;
        var resolvedLimit = limit != null ? limit : 50;
        var resolvedSearchField = searchField != null ? searchField : CardSearchField.NAME;
        var resolvedShowAllVariants = Boolean.TRUE.equals(showAllVariants);

        var resolvedName = name;
        var resolvedCosts = costs;
        var resolvedPower = power;
        var resolvedCounterAmount = counterAmount;
        var resolvedErrataOnly = false;

        // SEMANTIC mode: pull inline "6c"/"2kc"/"5kp" tokens out of the free text into the same
        // cost/power/counterAmount filters the sidebar uses - an explicit sidebar value always wins
        // over a token - then full-text search/rank whatever text remains (see JooqSetCardQueryAdapter).
        // The standalone "errata" keyword is also pulled out and turned into an errata-only filter.
        if (resolvedSearchField == CardSearchField.SEMANTIC && name != null && !name.isBlank()) {
            var parsed = semanticQueryParser.parse(name);
            if (parsed.cost() != null && (costs == null || costs.isEmpty())) {
                resolvedCosts = List.of(parsed.cost());
            }
            if (parsed.power() != null && power == null) {
                resolvedPower = parsed.power();
            }
            if (parsed.counter() != null && counterAmount == null) {
                resolvedCounterAmount = parsed.counter();
            }
            resolvedName = parsed.remainingText();
            resolvedErrataOnly = parsed.errataOnly();
        }

        var pageContent = setCardRepository.search(resolvedName, resolvedSearchField, types, colors, rarities, flatRarities, resolvedCosts, resolvedPower, resolvedCounterAmount, attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, resolvedPage, resolvedLimit, resolvedShowAllVariants, resolvedErrataOnly);
        var totalCount = setCardRepository.countSearch(resolvedName, resolvedSearchField, types, colors, rarities, flatRarities, resolvedCosts, resolvedPower, resolvedCounterAmount, attributes, attributeCombos, subTypes, prefixes, resolvedShowAllVariants, resolvedErrataOnly);

        return new PagedCards(pageContent, totalCount, resolvedPage, resolvedLimit);
    }

    public CardFilterOptions getFilterOptions() {
        return cardFilterOptionService.getFilterOptions();
    }
}
