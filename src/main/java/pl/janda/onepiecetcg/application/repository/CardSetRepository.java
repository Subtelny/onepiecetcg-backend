package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.CardSet;

import java.util.List;
import java.util.Optional;

public interface CardSetRepository {

    List<CardSet> findAll();

    Optional<CardSet> findById(String setId);

    <S extends CardSet> List<S> saveAll(Iterable<S> cardSets);
}
