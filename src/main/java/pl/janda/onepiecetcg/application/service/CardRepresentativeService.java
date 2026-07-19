package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardRepresentativeService {

    public static final Comparator<SetCard> CANONICAL_VARIANT_ORDER = Comparator
            .comparingInt((SetCard card) -> card.getCardImage() != null ? 0 : 1)
            .thenComparingInt((SetCard card) -> rarityRank(card.getFlatRarity()))
            .thenComparingInt((SetCard card) -> nameLength(card.getCardName()))
            .thenComparing(SetCard::getId, Comparator.reverseOrder());

    private final SetCardRepository setCardRepository;

    @Transactional
    public void recompute() {
        var cards = setCardRepository.findAll();

        var byCardNumber = new LinkedHashMap<String, List<SetCard>>();
        for (var card : cards) {
            var key = card.getCardSetId() != null ? card.getCardSetId() : "id:" + card.getId();
            byCardNumber.computeIfAbsent(key, k -> new ArrayList<>()).add(card);
        }

        for (var group : byCardNumber.values()) {
            var representative = group.stream().min(CANONICAL_VARIANT_ORDER).orElseThrow();
            group.forEach(card -> card.setRepresentative(card == representative));
        }

        setCardRepository.saveAll(cards);
        log.info("Recomputed representative flag for {} card groups ({} cards)", byCardNumber.size(), cards.size());
    }

    private static int rarityRank(String rarity) {
        try {
            return CardRarity.valueOf(rarity).ordinal();
        } catch (IllegalArgumentException | NullPointerException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static int nameLength(String cardName) {
        return cardName != null ? cardName.length() : Integer.MAX_VALUE;
    }
}
