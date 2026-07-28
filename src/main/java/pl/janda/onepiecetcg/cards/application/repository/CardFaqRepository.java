package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.CardFaq;

import java.util.List;

public interface CardFaqRepository {

    List<CardFaq> findAll();

    List<CardFaq> findBySetId(String setId);

    List<CardFaq> findByCardCodeIn(List<String> cardCodes);

    void deleteBySetId(String setId);

    <S extends CardFaq> List<S> saveAll(Iterable<S> faqEntries);
}
