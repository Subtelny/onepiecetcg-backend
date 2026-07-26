package pl.janda.onepiecetcg.application.client;

import pl.janda.onepiecetcg.application.model.CardErrata;

import java.util.List;

public interface CardErrataApiClient {

    List<CardErrata> fetchAllErrata();

}
