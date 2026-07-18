package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.application.client.SetCardApiClient;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SetCardSyncService {

    private final SetCardRepository setCardRepository;

    private final SetCardApiClient setCardApiClient;

    private final CardRepresentativeService cardRepresentativeService;

    private final CardFilterOptionService cardFilterOptionService;

    @Transactional
    public void syncSetCards() {
        var fetched = setCardApiClient.fetchAllSetCards();
        var now = LocalDateTime.now();
        fetched.forEach(setCard -> {
            setCard.setLastSyncedAt(now);
            setCard.setFlatRarity("P".equals(setCard.getCardPrefix()) ? "P" : setCard.getRarity());
        });
        setCardRepository.deleteByPromo(false);
        setCardRepository.saveAll(fetched);
        log.info("Synced {} set cards from optcgapi.com", fetched.size());
        cardRepresentativeService.recompute();
        cardFilterOptionService.refresh();
    }
}
