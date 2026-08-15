package pl.janda.onepiecetcg.pricing.application.repository;

import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;

import java.time.OffsetDateTime;
import java.util.List;

public interface CardmarketPriceCandidateRepository {

    boolean existsByPriceGuideCreatedAt(OffsetDateTime priceGuideCreatedAt);

    void saveAll(List<CardmarketPriceCandidate> candidates);
}
