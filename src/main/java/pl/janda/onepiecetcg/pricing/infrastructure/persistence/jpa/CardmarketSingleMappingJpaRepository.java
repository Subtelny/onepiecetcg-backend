package pl.janda.onepiecetcg.pricing.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketSingleMapping;

public interface CardmarketSingleMappingJpaRepository extends JpaRepository<CardmarketSingleMapping, Long> {
}
