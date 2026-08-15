package pl.janda.onepiecetcg.pricing.application.repository;

import pl.janda.onepiecetcg.pricing.application.model.CardmarketExpansion;

import java.util.List;

public interface CardmarketExpansionRepository {

    List<CardmarketExpansion> findAll();

    void saveAll(List<CardmarketExpansion> expansions);
}
