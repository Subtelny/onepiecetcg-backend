package pl.janda.onepiecetcg.pricing.application.client;

import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;

import java.util.List;

public interface CardmarketPriceApiClient {

    List<CardmarketPriceCandidate> fetchPriceCandidates();
}
