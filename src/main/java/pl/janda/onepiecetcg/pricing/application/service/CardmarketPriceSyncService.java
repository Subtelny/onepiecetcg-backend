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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardmarketPriceSyncService implements CardmarketPriceSyncUseCase {

    private final CardmarketPriceApiClient cardmarketPriceApiClient;

    private final PriceableSingleCatalogClient priceableSingleCatalogClient;

    private final CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;

    private final CardmarketExpansionRepository cardmarketExpansionRepository;

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

        var nonEnglishExpansionIds = cardmarketPriceApiClient.fetchNonEnglishExpansionIds();
        var excluded = Set.copyOf(nonEnglishExpansionIds);
        var englishCandidates = candidates.stream()
                .filter(candidate -> !excluded.contains(candidate.getExpansionId()))
                .toList();
        if (englishCandidates.isEmpty()) {
            throw new IllegalStateException("Every Cardmarket product was classified as non-English; keeping the existing snapshot");
        }

        var now = LocalDateTime.now();
        var singles = priceableSingleCatalogClient.fetchPriceableSingles();
        if (singles.isEmpty()) {
            throw new IllegalStateException("The card catalog exposed no priceable singles; keeping the existing mappings");
        }

        // The expansion matcher sees every candidate so a newly listed non-English expansion still gets a
        // row, only an unmapped one; everything downstream sees the English products alone.
        var expansions = cardmarketExpansionMatcher.match(
                cardmarketExpansionRepository.findAll(), candidates, singles, nonEnglishExpansionIds, now);
        var mappings = cardmarketSingleMatcher.match(englishCandidates, singles, expansions, now);

        var candidatesToAppend = guideAlreadyStored ? List.<CardmarketPriceCandidate>of() : englishCandidates;
        candidatesToAppend.forEach(candidate -> candidate.setLastSyncedAt(now));

        cardmarketPriceImportService.importPricingSnapshot(expansions, mappings, candidatesToAppend);
        log.info("Processed Cardmarket price guide created at {} with {} appended EUR price candidates, {} rebuilt single "
                        + "mappings and {} mapped expansions for {} card codes, after excluding {} non-English expansions "
                        + "covering {} products",
                priceGuideCreatedAt,
                candidatesToAppend.size(),
                mappings.size(),
                expansions.stream().filter(expansion -> expansion.getReleaseId() != null).count(),
                englishCandidates.stream().map(CardmarketPriceCandidate::getCardCode).distinct().count(),
                nonEnglishExpansionIds.size(),
                candidates.size() - englishCandidates.size());
    }
}
