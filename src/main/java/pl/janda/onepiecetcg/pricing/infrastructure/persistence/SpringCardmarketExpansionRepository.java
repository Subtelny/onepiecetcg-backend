package pl.janda.onepiecetcg.pricing.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketExpansionRepository;
import pl.janda.onepiecetcg.pricing.infrastructure.persistence.jpa.CardmarketExpansionJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringCardmarketExpansionRepository implements CardmarketExpansionRepository {

    private static final int BATCH_SIZE = 100;

    private final CardmarketExpansionJpaRepository jpaRepository;
    private final EntityManager entityManager;

    @Override
    public List<CardmarketExpansion> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void saveAll(List<CardmarketExpansion> expansions) {
        for (var i = 0; i < expansions.size(); i += BATCH_SIZE) {
            var endIndex = Math.min(i + BATCH_SIZE, expansions.size());
            jpaRepository.saveAllAndFlush(expansions.subList(i, endIndex));
            entityManager.clear();
        }
    }
}
