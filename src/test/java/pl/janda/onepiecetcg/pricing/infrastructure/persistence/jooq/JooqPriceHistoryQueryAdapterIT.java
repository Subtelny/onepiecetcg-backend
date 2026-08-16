package pl.janda.onepiecetcg.pricing.infrastructure.persistence.jooq;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardSetSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.model.PriceSource;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.repository.PriceHistoryRepository;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(classes = OnePieceTcgApplication.class)
class JooqPriceHistoryQueryAdapterIT extends PostgresSpringBootTest {

    private static final long PRODUCT_ID = 9_900_002L;
    private static final String PRICE_REFERENCE = "single:TEST-002_p1";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @MockitoBean
    private CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @MockitoBean
    private CardSetSyncUseCase cardSetSyncUseCase;

    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;

    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;

    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;

    @MockitoBean
    private MatchupSyncUseCase matchupSyncUseCase;

    @BeforeEach
    void setUp() {
        dsl.execute("DELETE FROM cardmarket_single_mappings WHERE cardmarket_product_id = ?", PRODUCT_ID);
        dsl.execute("DELETE FROM cardmarket_price_candidates WHERE product_id = ?", PRODUCT_ID);

        dsl.execute("""
                        INSERT INTO cardmarket_single_mappings (
                            cardmarket_product_id,
                            price_reference,
                            card_code,
                            expansion_id,
                            local_variant,
                            match_type,
                            confidence,
                            last_matched_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                PRODUCT_ID,
                PRICE_REFERENCE,
                "TEST-002",
                5585L,
                2,
                "CODE_EXPANSION_VERSION",
                new BigDecimal("1.000"),
                LocalDateTime.parse("2026-08-15T03:00:00"));
    }

    @Test
    void findHistoryByPriceReference_keepsOnlyObservationsWhereTrendOrLowMoved() {
        insertSnapshot("2026-08-11T04:00:00+02:00", new BigDecimal("35.00"), new BigDecimal("40.00"));
        // unchanged day - must not become a point
        insertSnapshot("2026-08-12T04:00:00+02:00", new BigDecimal("35.00"), new BigDecimal("40.00"));
        // only the low price moved - still a point
        insertSnapshot("2026-08-13T04:00:00+02:00", new BigDecimal("33.50"), new BigDecimal("40.00"));
        // only the trend price moved - still a point
        insertSnapshot("2026-08-14T04:00:00+02:00", new BigDecimal("33.50"), new BigDecimal("45.19"));
        insertSnapshot("2026-08-15T04:00:00+02:00", new BigDecimal("33.50"), new BigDecimal("45.19"));

        var history = priceHistoryRepository.findHistoryByPriceReference(PRICE_REFERENCE);

        assertThat(history)
                .extracting(point -> point.getObservedAt().toInstant(), point -> point.getTrendPrice().toPlainString(),
                        point -> point.getLowPrice().toPlainString())
                .containsExactly(
                        tuple(instant("2026-08-11T04:00:00+02:00"), "40.00", "35.00"),
                        tuple(instant("2026-08-13T04:00:00+02:00"), "40.00", "33.50"),
                        tuple(instant("2026-08-14T04:00:00+02:00"), "45.19", "33.50"));
        assertThat(history).allSatisfy(point -> {
            assertThat(point.getSource()).isEqualTo(PriceSource.CARDMARKET);
            assertThat(point.getCurrency()).isEqualTo("EUR");
        });
    }

    @Test
    void findHistoryByPriceReference_ignoresSnapshotsWithoutAnyTrackedPrice() {
        insertSnapshot("2026-08-11T04:00:00+02:00", new BigDecimal("35.00"), new BigDecimal("40.00"));
        insertSnapshot("2026-08-12T04:00:00+02:00", null, null);
        insertSnapshot("2026-08-13T04:00:00+02:00", new BigDecimal("35.00"), new BigDecimal("40.00"));

        var history = priceHistoryRepository.findHistoryByPriceReference(PRICE_REFERENCE);

        assertThat(history)
                .extracting(point -> point.getObservedAt().toInstant())
                .containsExactly(instant("2026-08-11T04:00:00+02:00"));
    }

    @Test
    void findHistoryByPriceReference_returnsEmptySeriesForAnUnmappedReference() {
        insertSnapshot("2026-08-11T04:00:00+02:00", new BigDecimal("35.00"), new BigDecimal("40.00"));

        assertThat(priceHistoryRepository.findHistoryByPriceReference("single:UNKNOWN")).isEmpty();
        assertThat(priceHistoryRepository.findHistoryByPriceReference(null)).isEmpty();
    }

    private static Instant instant(String observedAt) {
        return OffsetDateTime.parse(observedAt).toInstant();
    }

    private void insertSnapshot(String observedAt, BigDecimal lowPrice, BigDecimal trendPrice) {
        dsl.execute("""
                        INSERT INTO cardmarket_price_candidates (
                            product_id,
                            card_code,
                            expansion_id,
                            product_name,
                            price_guide_created_at,
                            average_price,
                            low_price,
                            trend_price,
                            last_synced_at
                        ) VALUES (?, ?, ?, ?, ?::timestamptz, ?, ?, ?, ?)
                        """,
                PRODUCT_ID,
                "TEST-002",
                5585L,
                "Test Card (TEST-002)",
                OffsetDateTime.parse(observedAt).toString(),
                new BigDecimal("44.00"),
                lowPrice,
                trendPrice,
                LocalDateTime.parse("2026-08-15T04:05:00"));
    }
}
