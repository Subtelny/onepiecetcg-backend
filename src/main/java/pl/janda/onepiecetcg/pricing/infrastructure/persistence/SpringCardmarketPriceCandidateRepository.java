package pl.janda.onepiecetcg.pricing.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketPriceCandidateRepository;
import pl.janda.onepiecetcg.pricing.infrastructure.persistence.jpa.CardmarketPriceCandidateJpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringCardmarketPriceCandidateRepository implements CardmarketPriceCandidateRepository {

    private static final int BATCH_SIZE = 100;

    private final CardmarketPriceCandidateJpaRepository jpaRepository;

    private final EntityManager entityManager;

    @Override
    public boolean existsByPriceGuideCreatedAt(OffsetDateTime priceGuideCreatedAt) {
        return jpaRepository.existsByPriceGuideCreatedAt(priceGuideCreatedAt);
    }

    @Override
    public void saveAll(List<CardmarketPriceCandidate> candidates) {
        for (var i = 0; i < candidates.size(); i += BATCH_SIZE) {
            var endIndex = Math.min(i + BATCH_SIZE, candidates.size());
            jpaRepository.saveAllAndFlush(candidates.subList(i, endIndex));
            entityManager.clear();
        }
    }
}
