package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.Card;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.repository.CardRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;

    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    public Card getCardById(String id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + id));
    }

    public Card getCardByCardNumber(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with number: " + cardNumber));
    }

    public List<Card> searchCards(
            String name,
            CardType type,
            List<CardColor> colors,
            List<CardRarity> rarities,
            Integer costMin,
            Integer costMax,
            Integer powerMin,
            Integer powerMax
    ) {
        return cardRepository.search(name, type, colors, rarities, costMin, costMax, powerMin, powerMax);
    }
}
