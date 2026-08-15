package pl.janda.onepiecetcg.pricing.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;

import java.time.OffsetDateTime;

public interface CardmarketPriceCandidateJpaRepository extends JpaRepository<CardmarketPriceCandidate, Long> {

    boolean existsByPriceGuideCreatedAt(OffsetDateTime priceGuideCreatedAt);
}
