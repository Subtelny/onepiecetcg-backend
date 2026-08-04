package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardRarity;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.repository.SetCardCommandRepository;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardRepresentativeService {

    public static final Comparator<SetCard> CANONICAL_VARIANT_ORDER = Comparator
            .comparingInt((SetCard card) -> card.getCardImage() != null ? 0 : 1)
            .thenComparingInt((SetCard card) -> rarityRank(card.getFlatRarity()))
            .thenComparingInt((SetCard card) -> nameLength(card.getCardName()))
            .thenComparing(SetCard::getId, Comparator.reverseOrder());

    private final SetCardCommandRepository setCardRepository;

    @Transactional
    public void recompute() {
        log.info("Starting recomputation of representative flags");
        setCardRepository.recomputeRepresentative();
        log.info("Completed recomputation of representative flags");
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
