package pl.janda.onepiecetcg.pricing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.pricing.application.client.CardmarketPriceApiClient;
import pl.janda.onepiecetcg.pricing.application.client.PriceableSingleCatalogClient;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketExpansionRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketPriceCandidateRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketSingleMappingRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardmarketPriceSyncService implements CardmarketPriceSyncUseCase {

    private final CardmarketPriceApiClient cardmarketPriceApiClient;

    private final PriceableSingleCatalogClient priceableSingleCatalogClient;

    private final CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;

    private final CardmarketExpansionRepository cardmarketExpansionRepository;

    private final CardmarketSingleMappingRepository cardmarketSingleMappingRepository;

    private final CardmarketExpansionMatcher cardmarketExpansionMatcher;

    private final CardmarketSingleMatcher cardmarketSingleMatcher;

    private final CardmarketPriceImportService cardmarketPriceImportService;

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
        var guideAlreadyStored = cardmarketPriceCandidateRepository.existsByPriceGuideCreatedAt(priceGuideCreatedAt);

        var now = LocalDateTime.now();
        var singles = priceableSingleCatalogClient.fetchPriceableSingles();
        var existingMappings = cardmarketSingleMappingRepository.findAll();

        var expansions = cardmarketExpansionMatcher.match(
                cardmarketExpansionRepository.findAll(), candidates, singles, now);
        var newMappings = cardmarketSingleMatcher.match(candidates, singles, expansions, existingMappings, now);

        var candidatesToAppend = guideAlreadyStored ? List.<CardmarketPriceCandidate>of() : candidates;
        candidatesToAppend.forEach(candidate -> candidate.setLastSyncedAt(now));

        cardmarketPriceImportService.append(expansions, newMappings, candidatesToAppend);
        log.info("Processed Cardmarket price guide created at {} with {} appended EUR price candidates, {} new single mappings "
                        + "and {} mapped expansions for {} card codes",
                priceGuideCreatedAt,
                candidatesToAppend.size(),
                newMappings.size(),
                expansions.stream().filter(expansion -> expansion.getReleaseId() != null).count(),
                candidates.stream().map(CardmarketPriceCandidate::getCardCode).distinct().count());
    }
}
