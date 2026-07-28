package pl.janda.onepiecetcg.cards.application.client;

import pl.janda.onepiecetcg.cards.application.model.CardSet;

import java.util.List;

public interface CardSetApiClient {

    List<CardSet> fetchAllSets();

}
