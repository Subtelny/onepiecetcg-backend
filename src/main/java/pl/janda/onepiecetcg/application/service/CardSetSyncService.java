package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.application.client.CardSetApiClient;
import pl.janda.onepiecetcg.application.repository.CardSetRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardSetSyncService {

    private final CardSetRepository cardSetRepository;

    private final CardSetApiClient cardSetApiClient;

    @Transactional
    public void syncCardSets() {
        var fetched = cardSetApiClient.fetchAllSets();
        var now = LocalDateTime.now();
        fetched.forEach(cardSet -> cardSet.setLastSyncedAt(now));
        cardSetRepository.saveAll(fetched);
        log.info("Synced {} card sets from optcgapi.com", fetched.size());
    }
}
