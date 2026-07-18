package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    // Picks the "basic" variant among cards sharing the same card number: lowest
    // rarity first, then non-promo over promo, then highest id as a fallback.
    // Expected to grow with more tie-breaking rules over time.
    private static final Comparator<SetCard> CANONICAL_VARIANT_ORDER = Comparator
            .comparingInt((SetCard card) -> rarityRank(card.getRarity()))
            .thenComparing(SetCard::isPromo)
            .thenComparing(SetCard::getId, Comparator.reverseOrder());

    private final SetCardRepository setCardRepository;

    private final CardFilterOptionService cardFilterOptionService;

    public List<SetCard> getAllCards() {
        return setCardRepository.findAll();
    }

    public SetCard getCardById(String id) {
        return setCardRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + id));
    }

    // All printed variants (different rarity/promo) sharing the same card number
    // as the given card, ordered the same way deduplicateVariants picks the canonical one.
    public List<SetCard> getVariantsByCardId(String id) {
        var card = getCardById(id);
        var cardSetId = card.getCardSetId();
        if (cardSetId == null) {
            return List.of(card);
        }
        return setCardRepository.findByCardSetId(cardSetId).stream()
                .sorted(CANONICAL_VARIANT_ORDER)
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
            Integer page,
            Integer limit
    ) {
        var resolvedPage = page != null ? page : 0;
        var resolvedLimit = limit != null ? limit : 50;

        var filtered = setCardRepository.search(name, types, colors, rarities, flatRarities, cost, power, counterAmount, attributes, subTypes, prefixes);
        var deduplicated = deduplicateVariants(filtered);

        var fromIndex = Math.min(resolvedPage * resolvedLimit, deduplicated.size());
        var toIndex = Math.min(fromIndex + resolvedLimit, deduplicated.size());
        var pageContent = deduplicated.subList(fromIndex, toIndex);

        return new PagedCards(pageContent, deduplicated.size(), resolvedPage, resolvedLimit);
    }

    public CardFilterOptions getFilterOptions() {
        return cardFilterOptionService.getFilterOptions();
    }

    // Same card number (e.g. "ST01-004") can appear multiple times as different
    // variants (different rarity/promo print) - keep only the canonical one.
    private static List<SetCard> deduplicateVariants(List<SetCard> cards) {
        var byCardNumber = new LinkedHashMap<String, SetCard>();
        for (var card : cards) {
            var key = card.getCardSetId() != null ? card.getCardSetId() : "id:" + card.getId();
            byCardNumber.merge(key, card,
                    (existing, candidate) -> CANONICAL_VARIANT_ORDER.compare(candidate, existing) < 0 ? candidate : existing);
        }
        return List.copyOf(byCardNumber.values());
    }

    private static int rarityRank(String rarity) {
        try {
            return CardRarity.valueOf(rarity).ordinal();
        } catch (IllegalArgumentException | NullPointerException e) {
            return Integer.MAX_VALUE;
        }
    }
}
