package pl.janda.onepiecetcg.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.application.model.CardFilterOptionValue;
import pl.janda.onepiecetcg.application.repository.CardFilterOptionRepository;

@Repository
public interface JpaCardFilterOptionRepository
        extends JpaRepository<CardFilterOptionValue, Long>, CardFilterOptionRepository {
}
