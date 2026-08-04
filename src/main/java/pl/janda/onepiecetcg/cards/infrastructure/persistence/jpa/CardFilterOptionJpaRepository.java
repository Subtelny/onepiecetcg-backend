package pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionValue;


public interface CardFilterOptionJpaRepository extends JpaRepository<CardFilterOptionValue, Long> {
}
