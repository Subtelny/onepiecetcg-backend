package pl.janda.onepiecetcg.cards.infrastructure.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.cards.application.client.CardSetApiClient;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.OptcgSetResponse;

import java.util.List;

@Component
public class OptcgApiCardSetClient extends AbstractOptcgApiClient implements CardSetApiClient {

    public OptcgApiCardSetClient(RestClient.Builder restClientBuilder,
                                  @Value("${optcgapi.base-url}") String baseUrl) {
        super(restClientBuilder, baseUrl);
    }

    @Override
    public List<CardSet> fetchAllSets() {
        return fetchAndMap("/allSets/", OptcgSetResponse[].class, r -> CardSet.builder()
                .setId(r.setId())
                .setName(r.setName())
                .build());
    }
}
