package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionCategory;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionValue;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.application.repository.CardFilterOptionRepository;
import pl.janda.onepiecetcg.cards.application.repository.CardSetRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardFilterOptionService {

    private final CardFilterOptionRepository cardFilterOptionRepository;

    private final CardSetRepository cardSetRepository;

    @Transactional
    public void refresh() {
        log.info("Starting refresh of card filter options cache");
        cardFilterOptionRepository.refresh();
        log.info("Completed refresh of card filter options cache");
    }

    private static List<CardSet> setsOf(
            List<CardFilterOptionValue> setEntries,
            Map<String, CardSet> cardSetsById
    ) {
        return setEntries.stream()
                .map(entry -> {
                    var source = cardSetsById.get(entry.getValue());
                    return CardSet.builder()
                            .setId(entry.getValue())
                            .setName(entry.getLabel())
                            .released(source == null || source.isReleased())
                            .releaseDate(source != null ? source.getReleaseDate() : null)
                            .build();
                })
                .sorted(Comparator.comparing(CardSet::getSetId))
                .toList();
    }

    private static List<String> valuesOf(Map<CardFilterOptionCategory, List<CardFilterOptionValue>> grouped,
                                          CardFilterOptionCategory category) {
        return grouped.getOrDefault(category, List.of()).stream()
                .map(CardFilterOptionValue::getValue)
                .sorted()
                .toList();
    }

    public CardFilterOptions getFilterOptions() {
        var grouped = cardFilterOptionRepository.findAll().stream()
                .collect(Collectors.groupingBy(CardFilterOptionValue::getCategory));

        var cardSetsById = cardSetRepository.findAll().stream()
                .collect(Collectors.toMap(CardSet::getSetId, Function.identity()));

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
                .sets(setsOf(grouped.getOrDefault(CardFilterOptionCategory.SET, List.of()), cardSetsById))
                .build();
    }
}
