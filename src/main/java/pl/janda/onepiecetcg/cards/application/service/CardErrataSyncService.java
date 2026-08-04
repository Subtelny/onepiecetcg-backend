package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.client.CardErrataApiClient;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class CardErrataSyncService implements CardErrataSyncUseCase {

    private final CardErrataApiClient cardErrataApiClient;

    private final CardErrataReplacementService cardErrataReplacementService;

    @Override
    public void syncErrata() {
        var fetched = cardErrataApiClient.fetchAllErrata();

        var now = LocalDateTime.now();
        fetched.forEach(errata -> errata.setLastSyncedAt(now));

        cardErrataReplacementService.replaceAll(fetched);
        log.info("Synced {} card errata entries from en.onepiece-cardgame.com", fetched.size());
    }
}
