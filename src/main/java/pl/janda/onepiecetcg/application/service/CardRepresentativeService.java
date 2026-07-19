package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class CardRepresentativeService {

    public static final Comparator<SetCard> CANONICAL_VARIANT_ORDER = Comparator
            .comparingInt((SetCard card) -> card.getCardImage() != null ? 0 : 1)
            .thenComparingInt((SetCard card) -> rarityRank(card.getFlatRarity()))
            .thenComparingInt((SetCard card) -> nameLength(card.getCardName()))
            .thenComparing(SetCard::getId, Comparator.reverseOrder());

    private final SetCardRepository setCardRepository;

    @Transactional
    public void recompute() {
        setCardRepository.recomputeRepresentative();
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
