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

    // Picks the "basic" variant among cards sharing the same card number: lowest
    // rarity first, then non-promo over promo, then highest id as a fallback.
    // Expected to grow with more tie-breaking rules over time.
    public static final Comparator<SetCard> CANONICAL_VARIANT_ORDER = Comparator
            .comparingInt((SetCard card) -> rarityRank(card.getRarity()))
            .thenComparing(SetCard::isPromo)
            .thenComparing(SetCard::getId, Comparator.reverseOrder());

    private final SetCardRepository setCardRepository;

    // Same card number (e.g. "ST01-004") can appear multiple times as different
    // variants (different rarity/promo print) - mark only the canonical one as
    // representative so search/filtering can rely on a precomputed flag instead
    // of recalculating the winner on every request.
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
}
