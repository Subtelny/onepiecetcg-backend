package pl.janda.onepiecetcg.pricing.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketSingleMapping;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketSingleMappingRepository;
import pl.janda.onepiecetcg.pricing.infrastructure.persistence.jpa.CardmarketSingleMappingJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringCardmarketSingleMappingRepository implements CardmarketSingleMappingRepository {

    private static final int BATCH_SIZE = 100;

    private final CardmarketSingleMappingJpaRepository jpaRepository;
    private final EntityManager entityManager;

    @Override
    public List<CardmarketSingleMapping> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void saveAll(List<CardmarketSingleMapping> mappings) {
        for (var i = 0; i < mappings.size(); i += BATCH_SIZE) {
            var endIndex = Math.min(i + BATCH_SIZE, mappings.size());
            jpaRepository.saveAllAndFlush(mappings.subList(i, endIndex));
            entityManager.clear();
        }
    }
}
