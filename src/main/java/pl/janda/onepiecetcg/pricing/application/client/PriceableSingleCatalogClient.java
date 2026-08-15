package pl.janda.onepiecetcg.pricing.application.client;

import pl.janda.onepiecetcg.pricing.application.model.PriceableSingle;

import java.util.List;

public interface PriceableSingleCatalogClient {

    List<PriceableSingle> fetchPriceableSingles();
}
