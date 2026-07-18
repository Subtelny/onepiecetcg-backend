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

    public List<SetCard> searchCards(
            String name,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            Integer cost,
            Integer power,
            List<String> setIds,
            Integer counterAmount,
            List<String> attributes,
            String subTypes,
            List<String> prefixes
    ) {
        return setCardRepository.search(name, types, colors, rarities, cost, power, setIds, counterAmount, attributes, subTypes, prefixes);
    }

    public CardFilterOptions getFilterOptions() {
        return cardFilterOptionService.getFilterOptions();
    }
}
