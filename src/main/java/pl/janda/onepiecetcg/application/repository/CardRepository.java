package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.Card;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;

import java.util.List;
import java.util.Optional;

public interface CardRepository {

    List<Card> findAll();

    Optional<Card> findById(String id);

    Optional<Card> findByCardNumber(String cardNumber);

    List<Card> search(
        String name,
        CardType type,
        List<CardColor> colors,
        List<CardRarity> rarities,
        Integer costMin,
        Integer costMax,
        Integer powerMin,
        Integer powerMax
    );
}
