package pl.janda.onepiecetcg.pricing.application.repository;

import pl.janda.onepiecetcg.pricing.application.model.PriceHistoryPoint;

import java.util.List;

public interface PriceHistoryRepository {

    List<PriceHistoryPoint> findHistoryByPriceReference(String priceReference);
}
