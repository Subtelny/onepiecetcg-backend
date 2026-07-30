package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.CardColor;
import pl.janda.onepiecetcg.cards.application.model.CardRarity;
import pl.janda.onepiecetcg.cards.application.model.CardSearchField;
import pl.janda.onepiecetcg.cards.application.model.CardSortField;
import pl.janda.onepiecetcg.cards.application.model.CardSummary;
import pl.janda.onepiecetcg.cards.application.model.CardType;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.model.SortDirection;

import java.util.List;
import java.util.Optional;

public interface SetCardRepository {

    void deleteAll();

    void saveAll(List<SetCard> setCards);

    Optional<SetCard> findById(Long id);

    List<SetCard> findByCardSetId(String cardSetId);

    List<String> findAllCardCodes();

    void recomputeRepresentative();

    boolean anyExist();

    List<CardSummary> search(
        String name,
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
        int page,
        int limit,
        boolean showAllVariants,
        boolean errataOnly
    );

    long countSearch(
        String name,
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
        boolean showAllVariants,
        boolean errataOnly
    );
}
