package pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;

import java.util.List;

public interface CardFaqJpaRepository extends JpaRepository<CardFaq, Long> {

    List<CardFaq> findBySetId(String setId);

    List<CardFaq> findByCardCodeIn(List<String> cardCodes);

    void deleteBySetId(String setId);
}
