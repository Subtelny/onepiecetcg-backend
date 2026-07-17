package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final SetCardRepository setCardRepository;

    public List<SetCard> getAllCards() {
        return setCardRepository.findAll();
    }

    public SetCard getCardById(String id) {
        return setCardRepository.findById(Long.valueOf(id))
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + id));
    }

    public List<SetCard> searchCards(
            String name,
            CardType type,
            List<CardColor> colors,
            List<CardRarity> rarities,
            Integer cost,
            Integer power,
            String setId,
            Integer counterAmount,
            String attribute,
            String subTypes
    ) {
        return setCardRepository.search(name, type, colors, rarities, cost, power, setId, counterAmount, attribute, subTypes);
    }
}
