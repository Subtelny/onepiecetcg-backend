package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.repository.SetCardRepository;

import java.util.List;

/**
 * Owns the transactional write half of the set-cards sync: replace the table contents, then recompute
 * the two derived artifacts that depend on them (the representative flag, then the filter-options
 * cache - in that order, since filter options are derived from the already-recomputed flag).
 * <p>
 * Split out of SetCardSyncService so the transaction starts here, after the outbound fetch of the full
 * catalog has already completed. Keeping the fetch outside means a multi-minute HTTP call no longer
 * holds a database connection and the delete's row locks for its entire duration.
 * <p>
 * This is a transaction boundary, not an abstraction for reuse - a delete-all followed by a bulk
 * insert must be atomic, or a failure part-way through leaves the card catalog empty or truncated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SetCardReplacementService {

    private final SetCardRepository setCardRepository;

    private final CardRepresentativeService cardRepresentativeService;

    private final CardFilterOptionService cardFilterOptionService;

    @Transactional
    public void replaceAll(List<SetCard> setCards) {
        log.info("Deleting existing set cards from database");
        var deleteStartTime = System.currentTimeMillis();
        setCardRepository.deleteAll();
        var deleteDuration = System.currentTimeMillis() - deleteStartTime;
        log.info("Deleted existing set cards in {}ms", deleteDuration);

        log.info("Saving {} set cards to database (this may take several minutes for large datasets)", setCards.size());
        var saveStartTime = System.currentTimeMillis();
        var saved = setCardRepository.saveAll(setCards);
        var saveDuration = System.currentTimeMillis() - saveStartTime;
        log.info("Successfully saved {} set cards to database in {}ms ({} seconds)",
                saved.size(), saveDuration, saveDuration / 1000);

        log.info("Recomputing representative flags for card variants");
        var recomputeStartTime = System.currentTimeMillis();
        cardRepresentativeService.recompute();
        var recomputeDuration = System.currentTimeMillis() - recomputeStartTime;
        log.info("Representative flags recomputed successfully in {}ms", recomputeDuration);

        log.info("Refreshing card filter options cache");
        var refreshStartTime = System.currentTimeMillis();
        cardFilterOptionService.refresh();
        var refreshDuration = System.currentTimeMillis() - refreshStartTime;
        log.info("Card filter options cache refreshed successfully in {}ms", refreshDuration);

        log.info("Set cards replacement completed - Breakdown: delete={}ms, save={}ms, recompute={}ms, refresh={}ms",
                deleteDuration, saveDuration, recomputeDuration, refreshDuration);
    }
}
