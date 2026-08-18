package pl.janda.onepiecetcg.pricing.application.repository;

import pl.janda.onepiecetcg.pricing.application.model.CardmarketSingleMapping;

import java.util.List;

public interface CardmarketSingleMappingRepository {

    void deleteAll();

    void saveAll(List<CardmarketSingleMapping> mappings);
}
