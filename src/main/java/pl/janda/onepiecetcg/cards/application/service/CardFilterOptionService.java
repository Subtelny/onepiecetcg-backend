package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionCategory;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionValue;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.application.repository.CardFilterOptionRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardFilterOptionService {

    private final CardFilterOptionRepository cardFilterOptionRepository;

    @Transactional
    public void refresh() {
        cardFilterOptionRepository.refresh();
    }

    public CardFilterOptions getFilterOptions() {
        var grouped = cardFilterOptionRepository.findAll().stream()
                .collect(Collectors.groupingBy(CardFilterOptionValue::getCategory));

        return CardFilterOptions.builder()
                .types(valuesOf(grouped, CardFilterOptionCategory.TYPE))
                .colors(valuesOf(grouped, CardFilterOptionCategory.COLOR))
                .rarities(valuesOf(grouped, CardFilterOptionCategory.RARITY))
                .flatRarities(valuesOf(grouped, CardFilterOptionCategory.FLAT_RARITY))
                .costs(valuesOf(grouped, CardFilterOptionCategory.COST))
                .attributes(valuesOf(grouped, CardFilterOptionCategory.ATTRIBUTE))
                .attributeCombos(valuesOf(grouped, CardFilterOptionCategory.ATTRIBUTE_COMBO))
                .subTypes(valuesOf(grouped, CardFilterOptionCategory.SUB_TYPE))
                .prefixes(valuesOf(grouped, CardFilterOptionCategory.PREFIX))
                .sets(setsOf(grouped.getOrDefault(CardFilterOptionCategory.SET, List.of())))
                .build();
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
