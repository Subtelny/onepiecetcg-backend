package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;

import java.util.List;
import java.util.Optional;

public interface SetCardRepository {

    List<SetCard> findAll();

    void deleteByPromo(boolean promo);

    <S extends SetCard> List<S> saveAll(Iterable<S> setCards);

    Optional<SetCard> findById(Long id);

    List<SetCard> search(
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
    );
}
