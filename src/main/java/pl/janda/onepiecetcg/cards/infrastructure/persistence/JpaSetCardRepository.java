package pl.janda.onepiecetcg.cards.infrastructure.persistence;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaSetCardRepository implements SetCardRepository {

    private final SetCardJpaRepository jpaRepository;

    private final JooqSetCardQueryAdapter jooqQueryAdapter;

    @Override
    public List<SetCard> findAll() {
        return jpaRepository.findAll();
    }

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
     */
    @Override
    public <S extends SetCard> List<S> saveAll(Iterable<S> setCards) {
        var list = new ArrayList<S>();
        setCards.forEach(list::add);
        var totalCount = list.size();

        log.info("Starting saveAllAndFlush for {} set cards", totalCount);
        var startTime = System.currentTimeMillis();

        try {
            // Process in batches to provide progress updates
            var batchSize = 50;
            var savedCards = new ArrayList<S>();

            for (var i = 0; i < list.size(); i += batchSize) {
                var endIndex = Math.min(i + batchSize, list.size());
                var batch = list.subList(i, endIndex);

                log.info("Saving batch {}/{} ({}-{} of {})",
                        (i / batchSize) + 1,
                        (totalCount + batchSize - 1) / batchSize,
                        i + 1,
                        endIndex,
                        totalCount);

                var batchStartTime = System.currentTimeMillis();
                var savedBatch = jpaRepository.saveAllAndFlush(batch);
                var batchDuration = System.currentTimeMillis() - batchStartTime;

                savedCards.addAll(savedBatch);
                log.info("Batch saved in {}ms ({} cards/sec)",
                        batchDuration,
                        batchDuration > 0 ? (batch.size() * 1000L / batchDuration) : 0);
            }

            var totalDuration = System.currentTimeMillis() - startTime;
            log.info("Completed saveAllAndFlush for {} set cards in {}ms ({} seconds, avg {} cards/sec)",
                    totalCount, totalDuration, totalDuration / 1000,
                    totalDuration > 0 ? (totalCount * 1000L / totalDuration) : 0);

            return savedCards;
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
    public boolean anyExist() {
        return jpaRepository.count() > 0;
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
