package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.application.client.PromoCardApiClient;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCardSyncService {

    private final SetCardRepository setCardRepository;

    private final PromoCardApiClient promoCardApiClient;

    private final CardFilterOptionService cardFilterOptionService;

    @Transactional
    public void syncPromoCards() {
        var fetched = promoCardApiClient.fetchAllPromoCards();
        var now = LocalDateTime.now();
        fetched.forEach(promoCard -> promoCard.setLastSyncedAt(now));
        setCardRepository.deleteByPromo(true);
        setCardRepository.saveAll(fetched);
        log.info("Synced {} promo cards from optcgapi.com", fetched.size());
        cardFilterOptionService.refresh();
    }
}
