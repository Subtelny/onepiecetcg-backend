package pl.janda.onepiecetcg.pricing.application.repository;

import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;

import java.util.List;

public interface PriceQuoteRepository {

    List<PriceQuote> findLatestByPriceReferences(List<String> priceReferences);
}
