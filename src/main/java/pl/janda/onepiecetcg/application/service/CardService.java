package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardSearchField;
import pl.janda.onepiecetcg.application.model.CardSortField;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.model.SortDirection;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final SetCardRepository setCardRepository;

    private final CardFilterOptionService cardFilterOptionService;

    public List<SetCard> getAllCards() {
        return setCardRepository.findAll();
    }

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
            List<String> effects,
            CardSortField sortBy,
            SortDirection sortOrder,
            Integer page,
            Integer limit
    ) {
        var resolvedPage = page != null ? page : 0;
        var resolvedLimit = limit != null ? limit : 50;
        var resolvedSearchField = searchField != null ? searchField : CardSearchField.NAME;

        var pageContent = setCardRepository.search(name, resolvedSearchField, types, colors, rarities, flatRarities, costs, power, counterAmount, attributes, attributeCombos, subTypes, prefixes, effects, sortBy, sortOrder, resolvedPage, resolvedLimit);
        var totalCount = setCardRepository.countSearch(name, resolvedSearchField, types, colors, rarities, flatRarities, costs, power, counterAmount, attributes, attributeCombos, subTypes, prefixes, effects);

        return new PagedCards(pageContent, totalCount, resolvedPage, resolvedLimit);
    }

    public CardFilterOptions getFilterOptions() {
        return cardFilterOptionService.getFilterOptions();
    }
}
