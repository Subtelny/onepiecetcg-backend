package pl.janda.onepiecetcg.infrastructure.client;

import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractOptcgApiClient {

    protected final RestClient restClient;

    protected AbstractOptcgApiClient(RestClient.Builder restClientBuilder, String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    protected <R, T> List<T> fetchAndMap(String uri, Class<R[]> responseType, Function<R, T> mapper) {
        var response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .map(mapper)
                .toList();
    }
}
