package pl.janda.onepiecetcg.cards.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.cards.application.client.CardmarketPriceApiClient;
import pl.janda.onepiecetcg.cards.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.CardmarketPriceGuideResponse;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.CardmarketPriceResponse;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.CardmarketProductCatalogResponse;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.CardmarketProductResponse;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CardmarketPriceClient implements CardmarketPriceApiClient {

    private static final Pattern CARD_CODE_PATTERN = Pattern.compile("\\(([A-Z]{1,5}\\d{0,2}-\\d{3})\\)");

    private final RestClient restClient;
    private final String productCatalogUrl;
    private final String priceGuideUrl;

    public CardmarketPriceClient(
            RestClient.Builder restClientBuilder,
            @Value("${cardmarket.product-catalog-url}") String productCatalogUrl,
            @Value("${cardmarket.price-guide-url}") String priceGuideUrl
    ) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(30));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = restClientBuilder
                .defaultHeader(HttpHeaders.USER_AGENT, "onepiecetcg-backend/1.0")
                .requestFactory(requestFactory)
                .build();
        this.productCatalogUrl = productCatalogUrl;
        this.priceGuideUrl = priceGuideUrl;
    }

    @Override
    public List<CardmarketPriceCandidate> fetchPriceCandidates() {
        log.info("Downloading Cardmarket One Piece product catalog");
        var products = getRequired(productCatalogUrl, CardmarketProductCatalogResponse.class, "product catalog");

        log.info("Downloading Cardmarket One Piece EUR price guide");
        var prices = getRequired(priceGuideUrl, CardmarketPriceGuideResponse.class, "price guide");

        var candidates = buildCandidates(products, prices);
        log.info("Mapped {} of {} Cardmarket products to Bandai card codes (catalog {}, price guide {})",
                candidates.size(), products.products().size(), products.version(), prices.version());
        return candidates;
    }

    private <T> T getRequired(String url, Class<T> responseType, String description) {
        var response = restClient.get().uri(url).retrieve().body(responseType);
        if (response == null) {
            throw new IllegalStateException("Cardmarket " + description + " response was empty");
        }
        return response;
    }

    static List<CardmarketPriceCandidate> buildCandidates(
            CardmarketProductCatalogResponse productCatalog,
            CardmarketPriceGuideResponse priceGuide
    ) {
        if (productCatalog.products() == null || productCatalog.products().isEmpty()) {
            throw new IllegalStateException("Cardmarket product catalog contains no products");
        }
        if (priceGuide.priceGuides() == null || priceGuide.priceGuides().isEmpty()) {
            throw new IllegalStateException("Cardmarket price guide contains no prices");
        }

        Map<Long, CardmarketPriceResponse> pricesByProduct = priceGuide.priceGuides().stream()
                .collect(Collectors.toMap(CardmarketPriceResponse::idProduct, Function.identity(), (first, second) -> second));
        var priceGuideCreatedAt = parseCardmarketTimestamp(priceGuide.createdAt());

        return productCatalog.products().stream()
                .map(product -> toCandidate(
                        product,
                        pricesByProduct.get(product.idProduct()),
                        productCatalog,
                        priceGuide,
                        priceGuideCreatedAt))
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<CardmarketPriceCandidate> toCandidate(
            CardmarketProductResponse product,
            CardmarketPriceResponse price,
            CardmarketProductCatalogResponse productCatalog,
            CardmarketPriceGuideResponse priceGuide,
            OffsetDateTime priceGuideCreatedAt
    ) {
        var matcher = CARD_CODE_PATTERN.matcher(product.name() == null ? "" : product.name());
        if (!matcher.find()) {
            return Optional.empty();
        }

        var candidate = CardmarketPriceCandidate.builder()
                .productId(product.idProduct())
                .cardCode(matcher.group(1))
                .expansionId(product.idExpansion())
                .metacardId(product.idMetacard())
                .productName(product.name())
                .dateAdded(product.dateAdded())
                .priceGuideVersion(priceGuide.version())
                .priceGuideCreatedAt(priceGuideCreatedAt)
                .productCatalogVersion(productCatalog.version())
                .productCatalogCreatedAt(productCatalog.createdAt())
                .build();

        if (price != null) {
            candidate.setAveragePrice(price.avg());
            candidate.setLowPrice(price.low());
            candidate.setTrendPrice(price.trend());
            candidate.setAveragePrice1Day(price.avg1());
            candidate.setAveragePrice7Days(price.avg7());
            candidate.setAveragePrice30Days(price.avg30());
            candidate.setFoilAveragePrice(price.avgFoil());
            candidate.setFoilLowPrice(price.lowFoil());
            candidate.setFoilTrendPrice(price.trendFoil());
            candidate.setFoilAveragePrice1Day(price.avg1Foil());
            candidate.setFoilAveragePrice7Days(price.avg7Foil());
            candidate.setFoilAveragePrice30Days(price.avg30Foil());
        }
        return Optional.of(candidate);
    }

    static OffsetDateTime parseCardmarketTimestamp(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Cardmarket price guide has no creation timestamp");
        }


        var normalized = value.replaceFirst("([+-]\\d{2})(\\d{2})$", "$1:$2");
        try {
            return OffsetDateTime.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Invalid Cardmarket price-guide creation timestamp: " + value, e);
        }
    }
}
