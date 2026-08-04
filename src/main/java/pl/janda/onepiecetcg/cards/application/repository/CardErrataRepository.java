package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.CardErrata;

import java.util.List;

public interface CardErrataRepository {

    List<CardErrata> findAll();

    void deleteAll();

    void saveAll(List<CardErrata> errata);

    List<CardErrata> findByCardCodeIn(List<String> cardCodes);
}
