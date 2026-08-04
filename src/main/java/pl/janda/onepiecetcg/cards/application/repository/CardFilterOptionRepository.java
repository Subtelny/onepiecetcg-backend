package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionValue;

import java.util.List;

public interface CardFilterOptionRepository {

    List<CardFilterOptionValue> findAll();

    void refresh();
}
