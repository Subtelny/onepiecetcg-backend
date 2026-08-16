package pl.janda.onepiecetcg.pricing.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.model.PriceSource;
import pl.janda.onepiecetcg.pricing.application.repository.PriceQuoteRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqPriceQuoteQueryAdapter implements PriceQuoteRepository {

    private final DSLContext dsl;

    @Override
    public List<PriceQuote> findLatestByPriceReferences(List<String> priceReferences) {
        if (priceReferences == null || priceReferences.isEmpty()) {
            return List.of();
        }
        var placeholders = String.join(", ", Collections.nCopies(priceReferences.size(), "?"));
        var records = dsl.fetch("""
                SELECT DISTINCT ON (mapping.price_reference)
                       mapping.price_reference,
                       price.product_id,
                       price.product_name,
                       price.average_price,
                       price.low_price,
                       price.trend_price,
                       price.price_guide_created_at
                FROM cardmarket_single_mappings mapping
                JOIN cardmarket_price_candidates price
                  ON price.product_id = mapping.cardmarket_product_id
                WHERE mapping.price_reference IN (%s)
                ORDER BY mapping.price_reference,
                         price.price_guide_created_at DESC,
                         price.id DESC
                """.formatted(placeholders), priceReferences.toArray());

        return records.map(record -> PriceQuote.builder()
                .priceReference(record.get("price_reference", String.class))
                .source(PriceSource.CARDMARKET)
                .currency("EUR")
                .externalProductId(String.valueOf(record.get("product_id", Long.class)))
                .productName(record.get("product_name", String.class))
                .averagePrice(record.get("average_price", BigDecimal.class))
                .lowPrice(record.get("low_price", BigDecimal.class))
                .trendPrice(record.get("trend_price", BigDecimal.class))
                .observedAt(record.get("price_guide_created_at", OffsetDateTime.class))
                .build());
    }
}
