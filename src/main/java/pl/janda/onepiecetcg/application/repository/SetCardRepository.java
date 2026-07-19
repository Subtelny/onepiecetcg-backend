package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardSearchField;
import pl.janda.onepiecetcg.application.model.CardSortField;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.model.SortDirection;

import java.util.List;
import java.util.Optional;

public interface SetCardRepository {

    List<SetCard> findAll();

    void deleteAll();

    <S extends SetCard> List<S> saveAll(Iterable<S> setCards);

    Optional<SetCard> findById(Long id);

    List<SetCard> findByCardSetId(String cardSetId);

    void recomputeRepresentative();

    List<SetCard> search(
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
        List<String> effects,
        CardSortField sortBy,
        SortDirection sortOrder,
        int page,
        int limit
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
        List<String> effects
    );
}
