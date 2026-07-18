package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.application.model.CardFilterOptionCategory;
import pl.janda.onepiecetcg.application.model.CardFilterOptionValue;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardSet;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.repository.CardFilterOptionRepository;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardFilterOptionService {

    private final SetCardRepository setCardRepository;

    private final CardFilterOptionRepository cardFilterOptionRepository;

    @Transactional
    public void refresh() {
        var cards = setCardRepository.findAll();

        var types = cards.stream()
                .map(SetCard::getCardType)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        var colors = cards.stream()
                .map(SetCard::getCardColor)
                .filter(Objects::nonNull)
                .flatMap(c -> Arrays.stream(c.split("\\s+")))
                .filter(token -> !token.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        var rarities = cards.stream()
                .map(SetCard::getRarity)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        var attributes = cards.stream()
                .map(SetCard::getAttribute)
                .filter(a -> a != null && !a.isBlank())
                .distinct()
                .toList();

        var subTypes = cards.stream()
                .map(SetCard::getSubTypes)
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split("\\s+")))
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();

        var prefixes = cards.stream()
                .map(SetCard::getCardPrefix)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        var sets = cards.stream()
                .filter(c -> c.getSetId() != null)
                .collect(Collectors.toMap(SetCard::getSetId,
                        c -> CardSet.builder().setId(c.getSetId()).setName(c.getSetName()).build(),
                        (a, b) -> a))
                .values();

        var entries = new ArrayList<CardFilterOptionValue>();
        types.forEach(t -> entries.add(entry(CardFilterOptionCategory.TYPE, t, null)));
        colors.forEach(c -> entries.add(entry(CardFilterOptionCategory.COLOR, c, null)));
        rarities.forEach(r -> entries.add(entry(CardFilterOptionCategory.RARITY, r, null)));
        attributes.forEach(a -> entries.add(entry(CardFilterOptionCategory.ATTRIBUTE, a, null)));
        subTypes.forEach(s -> entries.add(entry(CardFilterOptionCategory.SUB_TYPE, s, null)));
        prefixes.forEach(p -> entries.add(entry(CardFilterOptionCategory.PREFIX, p, null)));
        sets.forEach(s -> entries.add(entry(CardFilterOptionCategory.SET, s.getSetId(), s.getSetName())));

        cardFilterOptionRepository.deleteAll();
        cardFilterOptionRepository.saveAll(entries);
        log.info("Refreshed {} card filter option entries", entries.size());
    }

    public CardFilterOptions getFilterOptions() {
        var grouped = cardFilterOptionRepository.findAll().stream()
                .collect(Collectors.groupingBy(CardFilterOptionValue::getCategory));

        return CardFilterOptions.builder()
                .types(valuesOf(grouped, CardFilterOptionCategory.TYPE))
                .colors(valuesOf(grouped, CardFilterOptionCategory.COLOR))
                .rarities(valuesOf(grouped, CardFilterOptionCategory.RARITY))
                .attributes(valuesOf(grouped, CardFilterOptionCategory.ATTRIBUTE))
                .subTypes(valuesOf(grouped, CardFilterOptionCategory.SUB_TYPE))
                .prefixes(valuesOf(grouped, CardFilterOptionCategory.PREFIX))
                .sets(setsOf(grouped.getOrDefault(CardFilterOptionCategory.SET, List.of())))
                .build();
    }

    private static CardFilterOptionValue entry(CardFilterOptionCategory category, String value, String label) {
        return CardFilterOptionValue.builder().category(category).value(value).label(label).build();
    }

    private static List<String> valuesOf(Map<CardFilterOptionCategory, List<CardFilterOptionValue>> grouped,
                                          CardFilterOptionCategory category) {
        return grouped.getOrDefault(category, List.of()).stream()
                .map(CardFilterOptionValue::getValue)
                .sorted()
                .toList();
    }

    private static List<CardSet> setsOf(List<CardFilterOptionValue> setEntries) {
        return setEntries.stream()
                .map(entry -> CardSet.builder().setId(entry.getValue()).setName(entry.getLabel()).build())
                .sorted(Comparator.comparing(CardSet::getSetId))
                .toList();
    }
}
