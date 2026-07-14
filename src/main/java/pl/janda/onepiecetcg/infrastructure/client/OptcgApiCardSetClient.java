package pl.janda.onepiecetcg.infrastructure.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.application.client.CardSetApiClient;
import pl.janda.onepiecetcg.application.model.CardSet;
import pl.janda.onepiecetcg.infrastructure.client.dto.OptcgSetResponse;

import java.util.Arrays;
import java.util.List;

@Component
public class OptcgApiCardSetClient implements CardSetApiClient {

    private final RestClient restClient;

    public OptcgApiCardSetClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://www.optcgapi.com/api")
                .build();
    }

    @Override
    public List<CardSet> fetchAllSets() {
        OptcgSetResponse[] response = restClient.get()
                .uri("/allSets/")
                .retrieve()
                .body(OptcgSetResponse[].class);

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .map(r -> CardSet.builder()
                        .setId(r.setId())
                        .setName(r.setName())
                        .build())
                .toList();
    }
}
