package pl.janda.onepiecetcg.application.client;

import pl.janda.onepiecetcg.application.model.SetCard;

import java.util.List;

public interface PromoCardApiClient {

    List<SetCard> fetchAllPromoCards();

}
