package pl.janda.onepiecetcg.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.application.client.SetCardApiClient;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.infrastructure.client.dto.OptcgSetCardResponse;

import java.util.List;

@Component
public class OptcgApiSetCardClient extends AbstractSetCardApiClient implements SetCardApiClient {

    public OptcgApiSetCardClient(RestClient.Builder restClientBuilder,
                                  @Value("${optcgapi.base-url}") String baseUrl) {
        super(restClientBuilder, baseUrl);
    }

    @Override
    public List<SetCard> fetchAllSetCards() {
        return fetchAndMap("/allSetCards/", OptcgSetCardResponse[].class, r -> toSetCard(r, false));
    }
}
