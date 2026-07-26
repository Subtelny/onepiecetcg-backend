package pl.janda.onepiecetcg.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.application.model.CardErrata;
import pl.janda.onepiecetcg.application.repository.CardErrataRepository;

@Repository
public interface JpaCardErrataRepository extends JpaRepository<CardErrata, Long>, CardErrataRepository {
}
