package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlatRarityCalculatorService {

    private final FlatRarityOverrideProperties flatRarityOverrideProperties;

    public void assignFlatRarities(List<SetCard> cards) {
        var rarityByCardSetId = cards.stream()
                .filter(c -> !"PR".equals(c.getRarity()) && c.getCardSetId() != null && c.getRarity() != null)
                .collect(Collectors.toMap(SetCard::getCardSetId, SetCard::getRarity, (a, b) -> a));

        cards.forEach(card -> card.setFlatRarity(calculate(card, rarityByCardSetId)));
    }

    private String calculate(SetCard card, Map<String, String> rarityByCardSetId) {
        var override = flatRarityOverrideProperties.get(card.getCardSetId());
        if (override.isPresent()) {
            return override.get();
        }
        if (card.getLife() != null && !card.getLife().isBlank()) {
            return "L";
        }
        if ("P".equals(card.getCardPrefix())) {
            return "P";
        }
        if (card.getFlatRarity() != null && !card.getFlatRarity().isBlank()) {
            return card.getFlatRarity();
        }
        if ("PR".equals(card.getRarity())) {
            return rarityByCardSetId.getOrDefault(card.getCardSetId(), card.getRarity());
        }
        return card.getRarity();
    }
}
