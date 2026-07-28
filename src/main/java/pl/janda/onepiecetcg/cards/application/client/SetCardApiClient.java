package pl.janda.onepiecetcg.cards.application.client;

import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;

public interface SetCardApiClient {

    List<SetCard> fetchAllSetCards();

}
