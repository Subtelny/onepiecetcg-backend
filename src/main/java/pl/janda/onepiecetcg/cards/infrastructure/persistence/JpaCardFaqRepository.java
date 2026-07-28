package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;

@Repository
public interface JpaCardFaqRepository extends JpaRepository<CardFaq, Long>, CardFaqRepository {
}
