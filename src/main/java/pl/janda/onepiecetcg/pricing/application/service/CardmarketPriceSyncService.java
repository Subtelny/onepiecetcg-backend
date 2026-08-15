package pl.janda.onepiecetcg.pricing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.pricing.application.client.CardmarketPriceApiClient;
import pl.janda.onepiecetcg.pricing.application.client.CardmarketProductPageApiClient;
import pl.janda.onepiecetcg.pricing.application.client.PriceableSingleCatalogClient;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketProductPage;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketProductPageRequest;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketExpansionRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketPriceCandidateRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketSingleMappingRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardmarketPriceSyncService implements CardmarketPriceSyncUseCase {

    private final CardmarketPriceApiClient cardmarketPriceApiClient;

    private final CardmarketProductPageApiClient cardmarketProductPageApiClient;

    private final PriceableSingleCatalogClient priceableSingleCatalogClient;

    private final CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;

    private final CardmarketExpansionRepository cardmarketExpansionRepository;

    private final CardmarketSingleMappingRepository cardmarketSingleMappingRepository;

    private final CardmarketExpansionMatcher cardmarketExpansionMatcher;

    private final CardmarketSingleMatcher cardmarketSingleMatcher;

    private final CardmarketPriceImportService cardmarketPriceImportService;

    private static List<CardmarketProductPageRequest> expansionResolutionRequests(
            List<CardmarketPriceCandidate> candidates,
            Map<Long, CardmarketExpansion> expansionsById
    ) {
        return candidates.stream()
                .filter(candidate -> candidate.getExpansionId() != null && candidate.getProductId() != null)
                .filter(candidate -> {
                    var expansion = expansionsById.get(candidate.getExpansionId());
                    return expansion == null
                            || (expansion.getExpansionSlug() == null && expansion.getReleaseId() == null);
                })
                .collect(Collectors.groupingBy(CardmarketPriceCandidate::getExpansionId))
                .values().stream()
                .map(products -> products.stream()
                        .min(Comparator.comparing(CardmarketPriceCandidate::getProductId))
                        .orElseThrow())
                .map(product -> CardmarketProductPageRequest.builder()
                        .productId(product.getProductId())
                        .expansionId(product.getExpansionId())
                        .build())
                .toList();
    }

    private static void mergeExpansions(
            Map<Long, CardmarketExpansion> expansionsById,
            List<CardmarketProductPage> productPages,
            LocalDateTime resolvedAt
    ) {
        for (var page : productPages) {
            if (page.getExpansionId() == null || page.getExpansionSlug() == null) {
                continue;
            }
            var expansion = expansionsById.computeIfAbsent(page.getExpansionId(), expansionId ->
                    CardmarketExpansion.builder()
                            .expansionId(expansionId)
                            .lastResolvedAt(resolvedAt)
                            .build());
            if (expansion.getExpansionSlug() == null) {
                expansion.setExpansionSlug(page.getExpansionSlug());
                expansion.setLastResolvedAt(resolvedAt);
            }
        }
    }

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
        var expansionsById = cardmarketExpansionRepository.findAll().stream()
                .collect(Collectors.toMap(CardmarketExpansion::getExpansionId, Function.identity()));

        var expansionRequests = expansionResolutionRequests(candidates, expansionsById);
        var productPages = new ArrayList<>(cardmarketProductPageApiClient.resolveProductPages(expansionRequests));
        mergeExpansions(expansionsById, productPages, now);

        var expansions = new ArrayList<>(expansionsById.values());
        cardmarketExpansionMatcher.match(expansions, singles, now);

        var resolvedProductIds = productPages.stream()
                .map(CardmarketProductPage::getProductId)
                .collect(Collectors.toSet());
        var versionRequests = cardmarketSingleMatcher.findVersionResolutionRequests(
                        candidates, singles, expansions, existingMappings).stream()
                .filter(request -> !resolvedProductIds.contains(request.getProductId()))
                .toList();
        var versionPages = cardmarketProductPageApiClient.resolveProductPages(versionRequests);
        productPages.addAll(versionPages);
        mergeExpansions(expansionsById, versionPages, now);
        expansions = new ArrayList<>(expansionsById.values());
        cardmarketExpansionMatcher.match(expansions, singles, now);

        var newMappings = cardmarketSingleMatcher.match(
                candidates, singles, expansions, productPages, existingMappings, now);
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
