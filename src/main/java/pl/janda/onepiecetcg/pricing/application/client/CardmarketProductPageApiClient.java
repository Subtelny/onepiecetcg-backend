package pl.janda.onepiecetcg.pricing.application.client;

import pl.janda.onepiecetcg.pricing.application.model.CardmarketProductPage;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketProductPageRequest;

import java.util.List;

public interface CardmarketProductPageApiClient {

    List<CardmarketProductPage> resolveProductPages(List<CardmarketProductPageRequest> requests);
}
