package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardSearchCriteria;
import pl.janda.onepiecetcg.cards.application.model.CardSummary;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.repository.SetCardCommandRepository;
import pl.janda.onepiecetcg.cards.application.repository.SetCardQueryRepository;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jooq.JooqSetCardQueryAdapter;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa.SetCardJpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class SpringSetCardRepository implements SetCardQueryRepository, SetCardCommandRepository {

    private final SetCardJpaRepository jpaRepository;

    private final JooqSetCardQueryAdapter jooqQueryAdapter;

    private final EntityManager entityManager;


    @Override
    public void deleteAll() {
        jpaRepository.deleteAllInBatch();
    }


    @Override
    public void saveAll(List<SetCard> setCards) {
        var totalCount = setCards.size();

        log.info("Starting saveAllAndFlush for {} set cards", totalCount);
        var startTime = System.currentTimeMillis();

        try {

            var batchSize = 100;

            for (var i = 0; i < totalCount; i += batchSize) {
                var endIndex = Math.min(i + batchSize, totalCount);
                var batch = setCards.subList(i, endIndex);

                log.info("Saving batch {}/{} ({}-{} of {})",
                        (i / batchSize) + 1,
                        (totalCount + batchSize - 1) / batchSize,
                        i + 1,
                        endIndex,
                        totalCount);

                var batchStartTime = System.currentTimeMillis();
                jpaRepository.saveAllAndFlush(batch);
                entityManager.clear();
                var batchDuration = System.currentTimeMillis() - batchStartTime;

                log.info("Batch saved in {}ms ({} cards/sec)",
                        batchDuration,
                        batchDuration > 0 ? (batch.size() * 1000L / batchDuration) : 0);
            }

            var totalDuration = System.currentTimeMillis() - startTime;
            log.info("Completed saveAllAndFlush for {} set cards in {}ms ({} seconds, avg {} cards/sec)",
                    totalCount, totalDuration, totalDuration / 1000,
                    totalDuration > 0 ? (totalCount * 1000L / totalDuration) : 0);
        } catch (Exception e) {
            var duration = System.currentTimeMillis() - startTime;
            log.error("Error during saveAllAndFlush after {}ms: {}", duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<SetCard> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<SetCard> findByCardSetId(String cardSetId) {
        return jpaRepository.findByCardSetId(cardSetId);
    }

    @Override
    public Optional<SetCard> findByCardSetIdAndVariantIndex(String cardSetId, String variantIndex) {
        return jpaRepository.findByCardSetIdAndVariantIndex(cardSetId, variantIndex);
    }

    @Override
    public List<SetCard> findRepresentativesByCardSetIds(List<String> cardSetIds) {
        return jpaRepository.findByCardSetIdInAndVariantIndex(cardSetIds, SetCard.DEFAULT_VARIANT_INDEX);
    }

    @Override
    public List<String> findAllCardCodes() {
        return jpaRepository.findDistinctCardSetIds();
    }

    @Override
    public List<CardSummary> search(CardSearchCriteria criteria) {
        return jooqQueryAdapter.search(
                criteria.text(),
                criteria.searchField(),
                criteria.types(),
                criteria.colors(),
                criteria.rarities(),
                criteria.flatRarities(),
                criteria.costs(),
                criteria.power(),
                criteria.counterAmount(),
                criteria.attributes(),
                criteria.attributeCombos(),
                criteria.subTypes(),
                criteria.prefixes(),
                criteria.sortBy(),
                criteria.sortOrder(),
                criteria.page(),
                criteria.limit(),
                criteria.showAllVariants(),
                criteria.errataOnly());
    }

    @Override
    public long countSearch(CardSearchCriteria criteria) {
        return jooqQueryAdapter.countSearch(
                criteria.text(),
                criteria.searchField(),
                criteria.types(),
                criteria.colors(),
                criteria.rarities(),
                criteria.flatRarities(),
                criteria.costs(),
                criteria.power(),
                criteria.counterAmount(),
                criteria.attributes(),
                criteria.attributeCombos(),
                criteria.subTypes(),
                criteria.prefixes(),
                criteria.showAllVariants(),
                criteria.errataOnly());
    }
}
