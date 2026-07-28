package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardErrata;
import pl.janda.onepiecetcg.cards.application.repository.CardErrataRepository;

@Repository
public interface JpaCardErrataRepository extends JpaRepository<CardErrata, Long>, CardErrataRepository {
}
