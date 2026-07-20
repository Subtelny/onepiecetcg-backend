package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.application.client.CardSetApiClient;
import pl.janda.onepiecetcg.application.model.CardSet;
import pl.janda.onepiecetcg.application.repository.CardSetRepository;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardSetSyncService {

    private final CardSetRepository cardSetRepository;

    private final CardSetApiClient cardSetApiClient;

    /**
     * Syncs card sets only if a new set is detected vs. the local database.
     *
     * @return true if new sets were found and persisted, false if the sync was skipped
     */
    @Transactional
    public boolean syncCardSets() {
        var fetched = cardSetApiClient.fetchAllSets();
        var existingIds = cardSetRepository.findAll().stream()
                .map(CardSet::getSetId)
                .collect(Collectors.toSet());

        var newSets = fetched.stream()
                .filter(cardSet -> !existingIds.contains(cardSet.getSetId()))
                .toList();

        if (newSets.isEmpty()) {
            log.info("No new card sets detected from optcgapi.com, skipping sync");
            return false;
        }

        var now = LocalDateTime.now();
        fetched.forEach(cardSet -> cardSet.setLastSyncedAt(now));
        cardSetRepository.saveAll(fetched);
        log.info("Synced {} card sets from optcgapi.com ({} new: {})",
                fetched.size(), newSets.size(), newSets.stream().map(CardSet::getSetId).toList());
        return true;
    }
}
