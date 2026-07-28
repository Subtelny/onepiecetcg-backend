package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.application.repository.CardSetRepository;

@Repository
public interface JpaCardSetRepository extends JpaRepository<CardSet, String>, CardSetRepository {
}
