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

    @Transactional
    public void append(
            List<CardmarketExpansion> expansions,
            List<CardmarketSingleMapping> mappings,
            List<CardmarketPriceCandidate> prices
    ) {
        cardmarketExpansionRepository.saveAll(expansions);
        cardmarketSingleMappingRepository.saveAll(mappings);
        cardmarketPriceCandidateRepository.saveAll(prices);
    }
}
