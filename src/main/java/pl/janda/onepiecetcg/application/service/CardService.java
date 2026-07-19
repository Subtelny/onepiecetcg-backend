package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
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
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            Integer cost,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            String subTypes,
            List<String> prefixes,
            List<String> effects,
            Integer page,
            Integer limit
    ) {
        var resolvedPage = page != null ? page : 0;
        var resolvedLimit = limit != null ? limit : 50;

        var filtered = setCardRepository.search(name, types, colors, rarities, flatRarities, cost, power, counterAmount, attributes, subTypes, prefixes, effects);

        var fromIndex = Math.min(resolvedPage * resolvedLimit, filtered.size());
        var toIndex = Math.min(fromIndex + resolvedLimit, filtered.size());
        var pageContent = filtered.subList(fromIndex, toIndex);

        return new PagedCards(pageContent, filtered.size(), resolvedPage, resolvedLimit);
    }

    public CardFilterOptions getFilterOptions() {
        return cardFilterOptionService.getFilterOptions();
    }
}
