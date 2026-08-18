package pl.janda.onepiecetcg.pricing.infrastructure.persistence;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardSetSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketSingleMapping;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketSingleMatchType;
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.pricing.application.repository.CardmarketSingleMappingRepository;
import pl.janda.onepiecetcg.testsupport.PostgresSpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OnePieceTcgApplication.class)
class SpringCardmarketSingleMappingRepositoryIT extends PostgresSpringBootTest {

    private static final String PRICE_REFERENCE = "single:TEST-003_p1";

    @Autowired
    private DSLContext dsl;

    @Autowired
    private CardmarketSingleMappingRepository cardmarketSingleMappingRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

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

    private static CardmarketSingleMapping mapping(Long productId, Long expansionId) {
        return CardmarketSingleMapping.builder()
                .cardmarketProductId(productId)
                .priceReference(PRICE_REFERENCE)
                .cardCode("TEST-003")
                .expansionId(expansionId)
                .localVariant(2)
                .matchType(CardmarketSingleMatchType.CODE_EXPANSION_VERSION)
                .confidence(new BigDecimal("1.000"))
                .lastMatchedAt(LocalDateTime.parse("2026-08-15T03:00:00"))
                .build();
    }

    /**
     * The rebuild wipes the table, so the row it leaves behind would otherwise collide with a sibling
     * test class's fixture on the price-reference unique constraint.
     */
    @AfterEach
    void tearDown() {
        dsl.execute("DELETE FROM cardmarket_single_mappings WHERE price_reference = ?", PRICE_REFERENCE);
    }

    /**
     * The rebuild moves a price reference from the Japanese print run to its English twin, so the old and
     * the new row collide on the price-reference unique constraint unless the delete really is flushed
     * before the insert.
     */
    @Test
    void deleteAll_thenSaveAll_reassignsAPriceReferenceToADifferentProductInOneTransaction() {
        transactionTemplate.executeWithoutResult(status ->
                cardmarketSingleMappingRepository.saveAll(List.of(mapping(9_900_101L, 5580L))));

        transactionTemplate.executeWithoutResult(status -> {
            cardmarketSingleMappingRepository.deleteAll();
            cardmarketSingleMappingRepository.saveAll(List.of(mapping(9_900_102L, 5585L)));
        });

        var rows = dsl.fetch(
                "SELECT cardmarket_product_id, expansion_id FROM cardmarket_single_mappings WHERE price_reference = ?",
                PRICE_REFERENCE);
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("cardmarket_product_id")).isEqualTo(9_900_102L);
            assertThat(row.get("expansion_id")).isEqualTo(5585L);
        });
    }
}
