package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.client.CardErrataApiClient;

import java.time.LocalDateTime;

/**
 * Orchestrates the errata sync: scrape, stamp, then hand the result to CardErrataReplacementService for
 * the transactional write.
 * <p>
 * Deliberately not @Transactional - the expensive part is the scrape (one page fetch per errata notice),
 * and wrapping it held a database connection and the delete's locks for the whole run. The atomic part
 * is exactly the replace, which owns its own transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardErrataSyncService {

    private final CardErrataApiClient cardErrataApiClient;

    private final CardErrataReplacementService cardErrataReplacementService;

    public void syncErrata() {
        var fetched = cardErrataApiClient.fetchAllErrata();

        var now = LocalDateTime.now();
        fetched.forEach(errata -> errata.setLastSyncedAt(now));

        cardErrataReplacementService.replaceAll(fetched);
        log.info("Synced {} card errata entries from en.onepiece-cardgame.com", fetched.size());
    }
}
