package pl.janda.onepiecetcg.cards.application.model;

import java.util.List;


public record CardSearchQuery(
        String text,
        CardSearchField searchField,
        List<CardType> types,
        List<CardColor> colors,
        List<CardRarity> rarities,
        List<CardRarity> flatRarities,
        List<Integer> costs,
        Integer power,
        Integer counterAmount,
        List<String> attributes,
        List<String> attributeCombos,
        String subTypes,
        List<String> prefixes,
        CardSortField sortBy,
        SortDirection sortOrder,
        Integer page,
        Integer limit,
        Boolean showAllVariants
) {
}
