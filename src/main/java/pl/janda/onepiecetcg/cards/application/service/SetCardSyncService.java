package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Transactional
    public void syncSetCards(boolean force) {
        var hasNewSets = cardSetSyncService.syncCardSets();
        if (!hasNewSets && !force) {
            log.info("No new card sets detected, skipping set cards sync");
            return;
        }

        var fetched = setCardApiClient.fetchAllSetCards();
        var now = LocalDateTime.now();
        fetched.forEach(setCard -> setCard.setLastSyncedAt(now));
        flatRarityCalculatorService.assignFlatRarities(fetched);
        setCardRepository.deleteAll();
        var saved = setCardRepository.saveAll(fetched);
        log.info("Synced {} set cards from optcgapi.com{}", saved.size(), force ? " (forced)" : "");
        cardRepresentativeService.recompute();
        cardFilterOptionService.refresh();
    }
}
