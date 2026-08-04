package pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.cards.application.model.CardSet;

public interface CardSetJpaRepository extends JpaRepository<CardSet, String> {
}
