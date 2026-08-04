package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.client.CardmarketPriceApiClient;
import pl.janda.onepiecetcg.cards.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.cards.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.cards.application.repository.CardmarketPriceCandidateRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardmarketPriceSyncService implements CardmarketPriceSyncUseCase {

    private final CardmarketPriceApiClient cardmarketPriceApiClient;

    private final CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;

    private final CardmarketPriceHistoryService cardmarketPriceHistoryService;

    @Override
    public void syncPrices() {
        var candidates = cardmarketPriceApiClient.fetchPriceCandidates();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("Cardmarket returned no matching One Piece products; keeping the existing snapshot");
        }

        var priceGuideCreatedAt = candidates.getFirst().getPriceGuideCreatedAt();
        if (priceGuideCreatedAt == null) {
            throw new IllegalStateException("Cardmarket price guide has no creation timestamp; keeping the existing price history");
        }
        if (cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(priceGuideCreatedAt)) {
            log.info("Cardmarket price guide created at {} is already stored; skipping duplicate import", priceGuideCreatedAt);
            return;
        }

        var now = LocalDateTime.now();
        candidates.forEach(candidate -> candidate.setLastSyncedAt(now));

        cardmarketPriceHistoryService.appendAll(candidates);
        log.info("Appended Cardmarket price guide created at {} with {} EUR price candidates for {} card codes",
                priceGuideCreatedAt,
                candidates.size(),
                candidates.stream().map(CardmarketPriceCandidate::getCardCode).distinct().count());
    }
}
