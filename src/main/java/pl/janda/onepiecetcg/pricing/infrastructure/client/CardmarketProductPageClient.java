package pl.janda.onepiecetcg.pricing.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.pricing.application.client.CardmarketProductPageApiClient;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketProductPage;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketProductPageRequest;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CardmarketProductPageClient implements CardmarketProductPageApiClient {

    private static final Pattern VERSION_PATTERN = Pattern.compile("-V(?:\\.|-)?(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;
    private final String productPageUrlTemplate;

    public CardmarketProductPageClient(
            @Value("${cardmarket.product-page-url-template}") String productPageUrlTemplate
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.productPageUrlTemplate = productPageUrlTemplate;
    }

    static Optional<CardmarketProductPage> parseProductPage(
            CardmarketProductPageRequest product,
            URI canonicalUri
    ) {
        var segments = canonicalUri.getPath().split("/");
        for (var i = 0; i < segments.length - 2; i++) {
            if (!"Singles".equalsIgnoreCase(segments[i])) {
                continue;
            }
            var expansionSlug = decode(segments[i + 1]);
            var productSlug = decode(segments[i + 2]);
            var versionMatcher = VERSION_PATTERN.matcher(productSlug);
            var version = versionMatcher.find() ? Integer.valueOf(versionMatcher.group(1)) : null;
            return Optional.of(CardmarketProductPage.builder()
                    .productId(product.getProductId())
                    .expansionId(product.getExpansionId())
                    .expansionSlug(expansionSlug)
                    .version(version)
                    .build());
        }
        return Optional.empty();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public List<CardmarketProductPage> resolveProductPages(List<CardmarketProductPageRequest> requests) {
        var resolved = new ArrayList<CardmarketProductPage>();
        for (var request : requests) {
            resolveProductPage(request).ifPresent(resolved::add);
        }
        return resolved;
    }

    private Optional<CardmarketProductPage> resolveProductPage(CardmarketProductPageRequest product) {
        try {
            var uri = URI.create(productPageUrlTemplate.replace("{productId}", product.getProductId().toString()));
            var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("User-Agent", "onepiecetcg-backend/1.0")
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                log.warn("Cardmarket product {} resolved with HTTP {}", product.getProductId(), response.statusCode());
                return Optional.empty();
            }
            return parseProductPage(product, response.uri());
        } catch (Exception e) {
            log.warn("Could not resolve canonical Cardmarket URL for product {}: {}",
                    product.getProductId(), e.getMessage());
            return Optional.empty();
        }
    }
}
