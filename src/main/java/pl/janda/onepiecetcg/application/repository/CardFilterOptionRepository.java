package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.CardFilterOptionValue;

import java.util.List;

public interface CardFilterOptionRepository {

    List<CardFilterOptionValue> findAll();

    void deleteAll();

    <S extends CardFilterOptionValue> List<S> saveAll(Iterable<S> values);

    void refresh();
}
