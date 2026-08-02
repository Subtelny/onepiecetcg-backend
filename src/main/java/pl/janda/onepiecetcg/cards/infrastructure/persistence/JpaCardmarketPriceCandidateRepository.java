package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.cards.application.repository.CardmarketPriceCandidateRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaCardmarketPriceCandidateRepository implements CardmarketPriceCandidateRepository {

    private final CardmarketPriceCandidateJpaRepository jpaRepository;

    @Override
    public boolean existsByPriceGuideCreatedAt(OffsetDateTime priceGuideCreatedAt) {
        return jpaRepository.existsByPriceGuideCreatedAt(priceGuideCreatedAt);
    }

    @Override
    public <S extends CardmarketPriceCandidate> List<S> saveAll(Iterable<S> candidates) {
        return jpaRepository.saveAll(candidates);
    }
}
