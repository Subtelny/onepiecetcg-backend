package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.client.CardErrataApiClient;
import pl.janda.onepiecetcg.cards.application.repository.CardErrataRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardErrataSyncService {

    private final CardErrataRepository cardErrataRepository;

    private final CardErrataApiClient cardErrataApiClient;

    @Transactional
    public void syncErrata() {
        var fetched = cardErrataApiClient.fetchAllErrata();

        var now = LocalDateTime.now();
        fetched.forEach(errata -> errata.setLastSyncedAt(now));

        cardErrataRepository.deleteAll();
        cardErrataRepository.saveAll(fetched);
        log.info("Synced {} card errata entries from en.onepiece-cardgame.com", fetched.size());
    }
}
