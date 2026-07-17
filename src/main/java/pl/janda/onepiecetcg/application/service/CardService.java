package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardSet;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            Integer cost,
            Integer power,
            List<String> setIds,
            Integer counterAmount,
            List<String> attributes,
            String subTypes
    ) {
        return setCardRepository.search(name, types, colors, rarities, cost, power, setIds, counterAmount, attributes, subTypes);
    }

    public CardFilterOptions getFilterOptions() {
        var cards = setCardRepository.findAll();

        var types = cards.stream()
                .map(SetCard::getCardType)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();

        var colors = cards.stream()
                .map(SetCard::getCardColor)
                .filter(Objects::nonNull)
                .flatMap(c -> Arrays.stream(c.split("\\s+")))
                .filter(token -> !token.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();

        var rarities = cards.stream()
                .map(SetCard::getRarity)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .distinct()
                .sorted()
                .toList();

        var attributes = cards.stream()
                .map(SetCard::getAttribute)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .sorted()
                .toList();

        var subTypes = cards.stream()
                .map(SetCard::getSubTypes)
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split("\\s+")))
                .filter(token -> !token.isBlank())
                .distinct()
                .sorted()
                .toList();

        var sets = cards.stream()
                .filter(c -> c.getSetId() != null)
                .collect(Collectors.toMap(SetCard::getSetId,
                        c -> CardSet.builder().setId(c.getSetId()).setName(c.getSetName()).build(),
                        (a, b) -> a))
                .values().stream()
                .sorted(Comparator.comparing(CardSet::getSetId))
                .toList();

        return CardFilterOptions.builder()
                .types(types)
                .colors(colors)
                .rarities(rarities)
                .sets(sets)
                .attributes(attributes)
                .subTypes(subTypes)
                .build();
    }
}
