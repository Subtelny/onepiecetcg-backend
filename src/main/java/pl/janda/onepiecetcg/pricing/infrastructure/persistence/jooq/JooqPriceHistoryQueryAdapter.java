package pl.janda.onepiecetcg.pricing.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.pricing.application.model.PriceHistoryPoint;
import pl.janda.onepiecetcg.pricing.application.model.PriceSource;
import pl.janda.onepiecetcg.pricing.application.repository.PriceHistoryRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqPriceHistoryQueryAdapter implements PriceHistoryRepository {

    private final DSLContext dsl;

    /**
     * Collapses the append-only snapshot table into the observations that matter for charting: a row
     * is emitted when its trend or low price differs from the chronologically previous snapshot of the
     * same product (marking the start of a new price run), and the chronologically last snapshot is
     * always emitted as well — even when it didn't change anything — so the series always ends at "as
     * of the most recent sync" instead of at the last actual price movement. Without that trailing
     * point, a price that has been flat for weeks would render as a line that dead-ends weeks ago,
     * indistinguishable from a card that simply stopped syncing. Snapshots carrying neither price are
     * dropped before the comparison so a temporary gap in the source data cannot fabricate a price
     * movement.
     */
    @Override
    public List<PriceHistoryPoint> findHistoryByPriceReference(String priceReference) {
        if (priceReference == null || priceReference.isBlank()) {
            return List.of();
        }
        var records = dsl.fetch("""
                WITH snapshots AS (
                    SELECT price.price_guide_created_at AS observed_at,
                           price.trend_price,
                           price.low_price,
                           LAG(price.trend_price) OVER change_window AS previous_trend_price,
                           LAG(price.low_price) OVER change_window AS previous_low_price,
                           ROW_NUMBER() OVER (
                               ORDER BY price.price_guide_created_at DESC, price.id DESC
                           ) AS recency_rank
                    FROM cardmarket_single_mappings mapping
                    JOIN cardmarket_price_candidates price
                      ON price.product_id = mapping.cardmarket_product_id
                    WHERE mapping.price_reference = ?
                      AND (price.trend_price IS NOT NULL OR price.low_price IS NOT NULL)
                    WINDOW change_window AS (
                        ORDER BY price.price_guide_created_at, price.id
                    )
                )
                SELECT observed_at, trend_price, low_price
                FROM snapshots
                WHERE trend_price IS DISTINCT FROM previous_trend_price
                   OR low_price IS DISTINCT FROM previous_low_price
                   OR recency_rank = 1
                ORDER BY observed_at
                """, priceReference);

        return records.map(record -> PriceHistoryPoint.builder()
                .source(PriceSource.CARDMARKET)
                .currency("EUR")
                .observedAt(record.get("observed_at", OffsetDateTime.class))
                .trendPrice(record.get("trend_price", BigDecimal.class))
                .lowPrice(record.get("low_price", BigDecimal.class))
                .build());
    }
}
