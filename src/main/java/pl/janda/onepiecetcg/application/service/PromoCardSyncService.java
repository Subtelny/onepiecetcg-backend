package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.application.client.PromoCardApiClient;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCardSyncService {

    private final SetCardRepository setCardRepository;

    private final PromoCardApiClient promoCardApiClient;

    private final CardRepresentativeService cardRepresentativeService;

    private final CardFilterOptionService cardFilterOptionService;

    @Transactional
    public void syncPromoCards() {
        var fetched = promoCardApiClient.fetchAllPromoCards();
        var now = LocalDateTime.now();
        var flatRarityByCardSetId = flatRarityLookup();
        fetched.forEach(promoCard -> {
            promoCard.setLastSyncedAt(now);
            promoCard.setFlatRarity(flatRarityByCardSetId.getOrDefault(promoCard.getCardSetId(), promoCard.getRarity()));
        });
        setCardRepository.deleteByPromo(true);
        setCardRepository.saveAll(fetched);
        log.info("Synced {} promo cards from optcgapi.com", fetched.size());
        cardRepresentativeService.recompute();
        cardFilterOptionService.refresh();
    }

    private Map<String, String> flatRarityLookup() {
        return setCardRepository.findAll().stream()
                .filter(c -> c.getCardSetId() != null && c.getRarity() != null)
                .collect(Collectors.toMap(SetCard::getCardSetId, SetCard::getRarity, (a, b) -> a));
    }
}
