package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardColor;
import pl.janda.onepiecetcg.cards.application.model.CardRarity;
import pl.janda.onepiecetcg.cards.application.model.CardSearchField;
import pl.janda.onepiecetcg.cards.application.model.CardSortField;
import pl.janda.onepiecetcg.cards.application.model.CardSummary;
import pl.janda.onepiecetcg.cards.application.model.CardType;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.model.SortDirection;
import pl.janda.onepiecetcg.cards.application.repository.SetCardRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaSetCardRepository implements SetCardRepository {

    private final SetCardJpaRepository jpaRepository;

    private final JooqSetCardQueryAdapter jooqQueryAdapter;

    private final EntityManager entityManager;

    /**
     * Uses deleteAllInBatch (a single DELETE statement) rather than deleteAll, which loads every row
     * into the persistence context and deletes it one by one. Beyond the cost, that also mattered for
     * correctness: deleteAll only marks entities removed, and Hibernate's action queue executes
     * DELETEs last, so the deletes were still pending when the JOOQ recompute below read the table
     * over raw JDBC - see the flush note on saveAll and CLAUDE.md §3.
     */
    @Override
    public void deleteAll() {
        jpaRepository.deleteAllInBatch();
    }

    /**
     * Flushes each batch, because SetCardSyncService follows this with recomputeRepresentative() and
     * the filter-options refresh, both of which read set_cards through JOOQ (raw JDBC) inside the same
     * transaction. Hibernate cannot auto-flush ahead of a non-Hibernate-session query, so without the
     * flush those recomputes could run against an incomplete table - CLAUDE.md §3.
     * <p>
     * Clears the persistence context after each flush, so peak heap is one batch rather than the whole
     * catalog: without it every saved entity stays managed for the rest of the enclosing transaction,
     * each carrying an EntityEntry plus a loaded-state snapshot on top of the entity itself. Detaching
     * is safe here precisely because it happens after the flush - the rows are already in the database
     * for the rest of this transaction to see, and the two JOOQ recomputes that follow read raw SQL
     * rather than managed entities. Never reorder these two calls or drop the flush.
     * <p>
     * Returns void because the caller only ever needed the row count, which it already has from the
     * argument; collecting the saved entities to hand back was a second full copy of the catalog.
     */
    @Override
    public void saveAll(List<SetCard> setCards) {
        var totalCount = setCards.size();

        log.info("Starting saveAllAndFlush for {} set cards", totalCount);
        var startTime = System.currentTimeMillis();

        try {
            // Matches spring.jpa.properties.hibernate.jdbc.batch_size so a batch maps to one JDBC batch.
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
    public List<String> findAllCardCodes() {
        return jpaRepository.findDistinctCardSetIds();
    }

    @Override
    public void recomputeRepresentative() {
        jooqQueryAdapter.recomputeRepresentative();
    }

    @Override
    public List<CardSummary> search(
            String name,
            CardSearchField searchField,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            List<Integer> costs,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            CardSortField sortBy,
            SortDirection sortOrder,
            int page,
            int limit,
            boolean showAllVariants,
            boolean errataOnly
    ) {
        return jooqQueryAdapter.search(name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page, limit, showAllVariants, errataOnly);
    }

    @Override
    public long countSearch(
            String name,
            CardSearchField searchField,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            List<Integer> costs,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            boolean showAllVariants,
            boolean errataOnly
    ) {
        return jooqQueryAdapter.countSearch(name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, showAllVariants, errataOnly);
    }
}
