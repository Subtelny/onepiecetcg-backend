package pl.janda.onepiecetcg.pricing.application.port.in;

import pl.janda.onepiecetcg.pricing.application.model.PriceHistoryPoint;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;

import java.util.List;
import java.util.Map;

public interface PriceQueryUseCase {

    Map<String, List<PriceQuote>> getLatestPricesByReferences(List<String> priceReferences);

    List<PriceHistoryPoint> getPriceHistoryByReference(String priceReference);
}
