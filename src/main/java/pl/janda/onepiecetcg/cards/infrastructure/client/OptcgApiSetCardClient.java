package pl.janda.onepiecetcg.cards.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.cards.application.client.SetCardApiClient;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.OptcgSetCardResponse;

import java.util.List;
import java.util.stream.Stream;

@Component
public class OptcgApiSetCardClient extends AbstractSetCardApiClient implements SetCardApiClient {

    public OptcgApiSetCardClient(RestClient.Builder restClientBuilder,
                                  @Value("${optcgapi.base-url}") String baseUrl) {
        super(restClientBuilder, baseUrl);
    }

    @Override
    public List<SetCard> fetchAllSetCards() {
        var setCards = fetchAndMap("/allSetCards/", OptcgSetCardResponse[].class, r -> toSetCard(r, false));
        var stCards = fetchAndMap("/allSTCards/", OptcgSetCardResponse[].class, r -> toSetCard(r, false));
        var promoCards = fetchAndMap("/allPromos/", OptcgSetCardResponse[].class, r -> toSetCard(r, true));
        return Stream.concat(Stream.concat(setCards.stream(), stCards.stream()), promoCards.stream()).toList();
    }
}
