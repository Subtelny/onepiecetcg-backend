package pl.janda.onepiecetcg.cards.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Slf4j
public abstract class AbstractOptcgApiClient {

    protected final RestClient restClient;

    protected AbstractOptcgApiClient(RestClient.Builder restClientBuilder, String baseUrl) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        log.info("Initialized API client with base URL: {}", baseUrl);
    }

    protected <R, T> List<T> fetchAndMap(String uri, Class<R[]> responseType, Function<R, T> mapper) {
        log.info("Fetching data from URI: {}", uri);
        var response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(responseType);

        if (response == null) {
            log.warn("Received null response from URI: {}", uri);
            return List.of();
        }

        log.info("Received {} items from URI: {}", response.length, uri);
        var mapped = Arrays.stream(response)
                .map(mapper)
                .toList();
        log.info("Mapped {} items from URI: {}", mapped.size(), uri);

        return mapped;
    }
}
