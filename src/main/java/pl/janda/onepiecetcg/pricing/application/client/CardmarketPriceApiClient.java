package pl.janda.onepiecetcg.pricing.application.client;

import pl.janda.onepiecetcg.pricing.application.model.CardmarketPriceCandidate;

import java.util.List;

public interface CardmarketPriceApiClient {

    List<CardmarketPriceCandidate> fetchPriceCandidates();

    /**
     * Expansions whose print run is Japanese/Asia rather than English. Cardmarket lists both runs of a set
     * as separate expansions carrying identical card codes, so nothing in the singles feed distinguishes
     * them and they are indistinguishable to the matchers without this list.
     */
    List<Long> fetchNonEnglishExpansionIds();
}
