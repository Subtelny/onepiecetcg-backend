package pl.janda.onepiecetcg.application.client;

import pl.janda.onepiecetcg.application.model.CardSet;

import java.util.List;

public interface CardSetApiClient {

    List<CardSet> fetchAllSets();

}
