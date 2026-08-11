package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;

public interface SetCardCommandRepository {

    void lockForReplacement();

    void deleteAll();

    void saveAll(List<SetCard> setCards);
}
