package pl.janda.onepiecetcg.cards.application.client;

import pl.janda.onepiecetcg.cards.application.model.CardmarketPriceCandidate;

import java.util.List;

public interface CardmarketPriceApiClient {

    List<CardmarketPriceCandidate> fetchPriceCandidates();
}
