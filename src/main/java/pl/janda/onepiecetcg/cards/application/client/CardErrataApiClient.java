package pl.janda.onepiecetcg.cards.application.client;

import pl.janda.onepiecetcg.cards.application.model.CardErrata;

import java.util.List;

public interface CardErrataApiClient {

    List<CardErrata> fetchAllErrata();

}
