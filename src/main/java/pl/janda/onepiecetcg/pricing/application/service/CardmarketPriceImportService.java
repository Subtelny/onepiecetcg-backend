package pl.janda.onepiecetcg.pricing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketSingleMapping;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketExpansionRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketPriceCandidateRepository;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketSingleMappingRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardmarketPriceImportService {

    private final CardmarketExpansionRepository cardmarketExpansionRepository;
    private final CardmarketSingleMappingRepository cardmarketSingleMappingRepository;
    private final CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;

    /**
     * Not named for a single verb because the three writes differ on purpose: expansions are upserted,
     * single mappings are replaced wholesale, and price candidates are appended as immutable history.
     * The replace and the rebuild have to commit together, otherwise a failure halfway leaves every card
     * without a price, so they share this one transaction.
     */
    @Transactional
    public void importPricingSnapshot(
            List<CardmarketExpansion> expansions,
            List<CardmarketSingleMapping> mappings,
            List<CardmarketPriceCandidate> prices
    ) {
        cardmarketSingleMappingRepository.deleteAll();
        cardmarketExpansionRepository.saveAll(expansions);
        cardmarketSingleMappingRepository.saveAll(mappings);
        cardmarketPriceCandidateRepository.saveAll(prices);
    }
}
