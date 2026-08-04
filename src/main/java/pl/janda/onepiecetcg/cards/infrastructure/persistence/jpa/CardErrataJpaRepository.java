package pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.cards.application.model.CardErrata;

import java.util.List;

public interface CardErrataJpaRepository extends JpaRepository<CardErrata, Long> {

    List<CardErrata> findByCardCodeIn(List<String> cardCodes);
}
