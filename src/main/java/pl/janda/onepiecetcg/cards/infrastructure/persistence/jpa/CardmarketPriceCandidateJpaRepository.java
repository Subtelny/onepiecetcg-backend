package pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.cards.application.model.CardmarketPriceCandidate;

import java.time.OffsetDateTime;

public interface CardmarketPriceCandidateJpaRepository extends JpaRepository<CardmarketPriceCandidate, Long> {

    boolean existsByPriceGuideCreatedAt(OffsetDateTime priceGuideCreatedAt);
}
