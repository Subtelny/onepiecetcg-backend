package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.client.SetCardApiClient;
import pl.janda.onepiecetcg.cards.application.repository.SetCardRepository;

import java.time.LocalDateTime;

/**
 * Orchestrates the set-cards sync: decide whether to run, fetch, enrich, then hand the result to
 * SetCardReplacementService for the transactional write.
 * <p>
 * Deliberately not @Transactional. The expensive part is the outbound fetch of the whole catalog, and
 * wrapping it in a transaction previously held a connection and the delete's locks for the entire run.
 * The atomic part is exactly the replace, which owns its own transaction - see SetCardReplacementService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SetCardSyncService {

    private final SetCardRepository setCardRepository;

    private final SetCardApiClient setCardApiClient;

    private final FlatRarityCalculatorService flatRarityCalculatorService;

    private final SetCardReplacementService setCardReplacementService;

    private final CardSetSyncService cardSetSyncService;

    public void syncSetCards() {
        syncSetCards(false);
    }

    /**
     * Logs and swallows failures because it runs detached on an @Async thread with no caller to report
     * to. Safe to swallow here only because this method is not transactional: the write is atomic
     * inside SetCardReplacementService, so a failure rolls that transaction back rather than committing
     * a half-applied sync.
     */
    @Async
    public void syncSetCardsAsync(boolean force) {
        log.info("Starting async set cards sync in separate thread (force={})", force);
        try {
            syncSetCards(force);
            log.info("Async set cards sync completed successfully");
        } catch (Exception e) {
            log.error("Error during async set cards sync", e);
        }
    }

    public void syncSetCards(boolean force) {
        var startTime = System.currentTimeMillis();
        log.info("Set cards sync started (force={})", force);

        if (!shouldSync(force)) {
            return;
        }

        log.info("Fetching all set cards from optcgapi.com");
        var fetchStartTime = System.currentTimeMillis();
        var fetched = setCardApiClient.fetchAllSetCards();
        var fetchDuration = System.currentTimeMillis() - fetchStartTime;
        log.info("Fetched {} set cards from optcgapi.com in {}ms", fetched.size(), fetchDuration);

        log.info("Setting sync timestamp on fetched cards");
        var now = LocalDateTime.now();
        fetched.forEach(setCard -> setCard.setLastSyncedAt(now));

        log.info("Assigning flat rarities to {} cards", fetched.size());
        var rarityStartTime = System.currentTimeMillis();
        flatRarityCalculatorService.assignFlatRarities(fetched);
        var rarityDuration = System.currentTimeMillis() - rarityStartTime;
        log.info("Assigned flat rarities in {}ms", rarityDuration);

        setCardReplacementService.replaceAll(fetched);

        var totalDuration = System.currentTimeMillis() - startTime;
        log.info("Set cards sync completed successfully{} - Total time: {}ms ({} seconds), of which fetch={}ms, rarity={}ms",
                force ? " (forced)" : "", totalDuration, totalDuration / 1000, fetchDuration, rarityDuration);
    }

    /**
     * Gates the expensive fetch behind card-sets diff detection: unless forced, a run is only worth
     * doing when the external /allSets/ response contains a set that is missing locally.
     */
    private boolean shouldSync(boolean force) {
        if (force) {
            log.info("Force sync enabled, skipping new card sets check");
            return true;
        }
        log.info("Checking if new card sets exist before syncing set cards");
        if (!setCardRepository.anyExist()) {
            log.info("No set cards exist yet, proceeding with initial sync");
            return true;
        }
        log.info("Set cards already exist, checking for new card sets");
        if (!cardSetSyncService.syncCardSets()) {
            log.info("No new card sets detected, skipping set cards sync");
            return false;
        }
        log.info("New card sets detected, proceeding with set cards sync");
        return true;
    }
}
