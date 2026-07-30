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
        var fetched = setCardApiClient.fetchAllSetCards();
        log.info("Fetched {} set cards from optcgapi.com", fetched.size());

        log.info("Setting sync timestamp on fetched cards");
        var now = LocalDateTime.now();
        fetched.forEach(setCard -> setCard.setLastSyncedAt(now));

        log.info("Assigning flat rarities to {} cards", fetched.size());
        flatRarityCalculatorService.assignFlatRarities(fetched);

        log.info("Deleting existing set cards from database");
        setCardRepository.deleteAll();

        log.info("Saving {} set cards to database", fetched.size());
        var saved = setCardRepository.saveAll(fetched);
        log.info("Successfully saved {} set cards to database{}", saved.size(), force ? " (forced)" : "");

        log.info("Recomputing representative flags for card variants");
        cardRepresentativeService.recompute();
        log.info("Representative flags recomputed successfully");

        log.info("Refreshing card filter options cache");
        cardFilterOptionService.refresh();
        log.info("Card filter options cache refreshed successfully");

        log.info("Set cards sync completed successfully (synced {} cards)", saved.size());
    }
}
