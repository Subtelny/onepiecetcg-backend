package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.CardSet;

import java.util.List;
import java.util.Optional;

public interface CardSetRepository {

    List<CardSet> findAll();

    Optional<CardSet> findById(String setId);

    void saveAll(List<CardSet> cardSets);

    void deleteAll(List<CardSet> cardSets);
}
