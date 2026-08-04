package pl.janda.onepiecetcg.cards.infrastructure.client;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.CardmarketPriceGuideResponse;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.CardmarketPriceResponse;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.CardmarketProductCatalogResponse;
import pl.janda.onepiecetcg.cards.infrastructure.client.dto.CardmarketProductResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardmarketPriceClientTest {

    @Test
    void buildCandidates_groupsEveryPrintingByCardCode_andJoinsPricesByProductId() {
        var products = new CardmarketProductCatalogResponse("1", "2026-08-01", List.of(
                new CardmarketProductResponse(101L, 10L, 1001L, "Monkey.D.Luffy (OP01-001)", "2022-01-01"),
                new CardmarketProductResponse(102L, 11L, 1001L, "Monkey.D.Luffy V.2 (OP01-001)", "2023-01-01"),
                new CardmarketProductResponse(103L, 12L, 1002L, "DON!! Card", "2023-01-01"),
                new CardmarketProductResponse(104L, 13L, 1003L, "Nami (ST01-001)", "2023-01-01")
        ));
        var prices = new CardmarketPriceGuideResponse("2", "2026-08-02T02:44:43+0200", List.of(
                price(101L, "1.10", "0.50", "0.90"),
                price(102L, "9.00", "8.00", "8.50")
        ));

        var result = CardmarketPriceClient.buildCandidates(products, prices);

        assertThat(result).hasSize(3);
        assertThat(result).filteredOn(candidate -> candidate.getCardCode().equals("OP01-001")).hasSize(2);
        assertThat(result).extracting(candidate -> candidate.getProductId()).containsExactly(101L, 102L, 104L);

        var firstPrinting = result.getFirst();
        assertThat(firstPrinting.getAveragePrice()).isEqualByComparingTo("1.10");
        assertThat(firstPrinting.getLowPrice()).isEqualByComparingTo("0.50");
        assertThat(firstPrinting.getTrendPrice()).isEqualByComparingTo("0.90");
        assertThat(firstPrinting.getFoilAveragePrice()).isEqualByComparingTo("2.10");
        assertThat(firstPrinting.getPriceGuideVersion()).isEqualTo("2");
        assertThat(firstPrinting.getPriceGuideCreatedAt())
                .isEqualTo(OffsetDateTime.parse("2026-08-02T02:44:43+02:00"));
        assertThat(firstPrinting.getProductCatalogVersion()).isEqualTo("1");


        assertThat(result.getLast().getCardCode()).isEqualTo("ST01-001");
        assertThat(result.getLast().getAveragePrice()).isNull();
    }

    @Test
    void buildCandidates_rejectsAnEmptyGuide_insteadOfProducingADestructiveEmptySnapshot() {
        var products = new CardmarketProductCatalogResponse("1", "2026-08-01", List.of(
                new CardmarketProductResponse(101L, 10L, 1001L, "Luffy (OP01-001)", "2022-01-01")
        ));
        var prices = new CardmarketPriceGuideResponse("2", "2026-08-01", List.of());

        assertThatThrownBy(() -> CardmarketPriceClient.buildCandidates(products, prices))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no prices");
    }

    @Test
    void parseCardmarketTimestamp_acceptsCompactAndColonOffsets() {
        assertThat(CardmarketPriceClient.parseCardmarketTimestamp("2026-08-02T02:44:43+0200"))
                .isEqualTo(OffsetDateTime.parse("2026-08-02T02:44:43+02:00"));
        assertThat(CardmarketPriceClient.parseCardmarketTimestamp("2026-08-02T02:44:43+02:00"))
                .isEqualTo(OffsetDateTime.parse("2026-08-02T02:44:43+02:00"));
    }

    private static CardmarketPriceResponse price(Long productId, String average, String low, String trend) {
        return new CardmarketPriceResponse(
                productId,
                new BigDecimal(average),
                new BigDecimal(low),
                new BigDecimal(trend),
                new BigDecimal("1.00"),
                new BigDecimal("1.01"),
                new BigDecimal("1.02"),
                new BigDecimal("2.10"),
                new BigDecimal("2.00"),
                new BigDecimal("2.05"),
                new BigDecimal("2.01"),
                new BigDecimal("2.02"),
                new BigDecimal("2.03")
        );
    }
}
