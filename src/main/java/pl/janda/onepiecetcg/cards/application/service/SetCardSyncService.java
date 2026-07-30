package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.client.SetCardApiClient;
import pl.janda.onepiecetcg.cards.application.repository.SetCardRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetCardSyncService {

    private final SetCardRepository setCardRepository;

    private final SetCardApiClient setCardApiClient;

    private final FlatRarityCalculatorService flatRarityCalculatorService;

    private final CardRepresentativeService cardRepresentativeService;

    private final CardFilterOptionService cardFilterOptionService;

    private final CardSetSyncService cardSetSyncService;

    @Transactional
    public void syncSetCards() {
        syncSetCards(false);
    }

    @Async
    @Transactional
    public void syncSetCardsAsync(boolean force) {
        log.info("Starting async set cards sync in separate thread (force={})", force);
        try {
            performSyncSetCards(force);
            log.info("Async set cards sync completed successfully");
        } catch (Exception e) {
            log.error("Error during async set cards sync", e);
        }
    }

    @Transactional
    public void syncSetCards(boolean force) {
        performSyncSetCards(force);
    }

    private void performSyncSetCards(boolean force) {
        var startTime = System.currentTimeMillis();
        log.info("Set cards sync started (force={})", force);

        if (!force) {
            log.info("Checking if new card sets exist before syncing set cards");
            if (setCardRepository.anyExist()) {
                log.info("Set cards already exist, checking for new card sets");
                var hasNewSets = cardSetSyncService.syncCardSets();
                if (!hasNewSets) {
                    log.info("No new card sets detected, skipping set cards sync");
                    return;
                }
                log.info("New card sets detected, proceeding with set cards sync");
            } else {
                log.info("No set cards exist yet, proceeding with initial sync");
            }
        } else {
            log.info("Force sync enabled, skipping new card sets check");
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

        log.info("Deleting existing set cards from database");
        var deleteStartTime = System.currentTimeMillis();
        setCardRepository.deleteAll();
        var deleteDuration = System.currentTimeMillis() - deleteStartTime;
        log.info("Deleted existing set cards in {}ms", deleteDuration);

        log.info("Saving {} set cards to database (this may take several minutes for large datasets)", fetched.size());
        var saveStartTime = System.currentTimeMillis();

        try {
            log.info("Starting batch save operation...");
            var saved = setCardRepository.saveAll(fetched);
            var saveDuration = System.currentTimeMillis() - saveStartTime;
            log.info("Successfully saved {} set cards to database in {}ms ({} seconds){}",
                    saved.size(), saveDuration, saveDuration / 1000, force ? " (forced)" : "");
        } catch (Exception e) {
            var saveDuration = System.currentTimeMillis() - saveStartTime;
            log.error("Failed to save set cards after {}ms. Error: {}", saveDuration, e.getMessage(), e);
            throw e;
        }

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

        var totalDuration = System.currentTimeMillis() - startTime;
        log.info("Set cards sync completed successfully - Total time: {}ms ({} seconds) - Breakdown: fetch={}ms, rarity={}ms, delete={}ms, save={}ms, recompute={}ms, refresh={}ms",
                totalDuration, totalDuration / 1000, fetchDuration, rarityDuration, deleteDuration,
                (System.currentTimeMillis() - saveStartTime), recomputeDuration, refreshDuration);
    }
}
