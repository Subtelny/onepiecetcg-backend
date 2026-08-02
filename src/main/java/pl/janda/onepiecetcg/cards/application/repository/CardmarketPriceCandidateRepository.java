package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.CardmarketPriceCandidate;

import java.time.OffsetDateTime;
import java.util.List;

public interface CardmarketPriceCandidateRepository {

    boolean existsByPriceGuideCreatedAt(OffsetDateTime priceGuideCreatedAt);

    <S extends CardmarketPriceCandidate> List<S> saveAll(Iterable<S> candidates);
}
