package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa.CardFaqJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringCardFaqRepository implements CardFaqRepository {

    private final CardFaqJpaRepository jpaRepository;

    @Override
    public List<CardFaq> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<CardFaq> findBySetId(String setId) {
        return jpaRepository.findBySetId(setId);
    }

    @Override
    public List<CardFaq> findByCardCodeIn(List<String> cardCodes) {
        return jpaRepository.findByCardCodeIn(cardCodes);
    }

    @Override
    public void deleteBySetId(String setId) {
        jpaRepository.deleteBySetId(setId);
    }

    @Override
    public void saveAll(List<CardFaq> faqEntries) {
        jpaRepository.saveAll(faqEntries);
    }
}
