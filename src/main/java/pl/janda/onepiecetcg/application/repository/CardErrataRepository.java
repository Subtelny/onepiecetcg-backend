package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.CardErrata;

import java.util.List;

public interface CardErrataRepository {

    List<CardErrata> findAll();

    void deleteAll();

    <S extends CardErrata> List<S> saveAll(Iterable<S> errata);

    List<CardErrata> findByCardCodeIn(List<String> cardCodes);
}
