package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.CardFaq;

import java.util.List;

public interface CardFaqRepository {

    List<CardFaq> findAll();

    List<CardFaq> findBySetId(String setId);

    void deleteBySetId(String setId);

    <S extends CardFaq> List<S> saveAll(Iterable<S> faqEntries);
}
