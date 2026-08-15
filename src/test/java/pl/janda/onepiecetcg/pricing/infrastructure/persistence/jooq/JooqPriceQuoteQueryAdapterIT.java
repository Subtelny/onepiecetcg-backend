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
import pl.janda.onepiecetcg.pricing.application.repository.PriceQuoteRepository;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OnePieceTcgApplication.class)
class JooqPriceQuoteQueryAdapterIT extends PostgresSpringBootTest {

    private static final long PRODUCT_ID = 9_900_001L;
    private static final String PRICE_REFERENCE = "single:TEST-001_p1";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private PriceQuoteRepository priceQuoteRepository;

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
                "TEST-001",
                5585L,
                2,
                "CODE_EXPANSION_VERSION",
                new BigDecimal("1.000"),
                LocalDateTime.parse("2026-08-15T03:00:00"));
        insertSnapshot(
                OffsetDateTime.parse("2026-08-14T04:00:00+02:00"),
                new BigDecimal("35.00"),
                new BigDecimal("40.00"));
        insertSnapshot(
                OffsetDateTime.parse("2026-08-15T04:00:00+02:00"),
                new BigDecimal("37.99"),
                new BigDecimal("45.19"));
    }

    @Test
    void findLatestByPriceReferences_returnsNewestSnapshotForMappedCard() {
        var prices = priceQuoteRepository.findLatestByPriceReferences(
                List.of(PRICE_REFERENCE, "single:UNKNOWN"));

        assertThat(prices).singleElement().satisfies(price -> {
            assertThat(price.getPriceReference()).isEqualTo(PRICE_REFERENCE);
            assertThat(price.getSource()).isEqualTo(PriceSource.CARDMARKET);
            assertThat(price.getCurrency()).isEqualTo("EUR");
            assertThat(price.getExternalProductId()).isEqualTo(String.valueOf(PRODUCT_ID));
            assertThat(price.getProductName()).isEqualTo("Test Card (TEST-001)");
            assertThat(price.getLowPrice()).isEqualByComparingTo("37.99");
            assertThat(price.getTrendPrice()).isEqualByComparingTo("45.19");
            assertThat(price.getObservedAt()).isEqualTo(OffsetDateTime.parse("2026-08-15T02:00:00Z"));
        });
    }

    private void insertSnapshot(OffsetDateTime observedAt, BigDecimal lowPrice, BigDecimal trendPrice) {
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
                "TEST-001",
                5585L,
                "Test Card (TEST-001)",
                observedAt.toString(),
                new BigDecimal("44.00"),
                lowPrice,
                trendPrice,
                LocalDateTime.parse("2026-08-15T04:05:00"));
    }
}
