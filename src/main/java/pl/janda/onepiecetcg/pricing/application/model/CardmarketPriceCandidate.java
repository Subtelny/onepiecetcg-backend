package pl.janda.onepiecetcg.pricing.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "cardmarket_price_candidates",
        indexes = {
                @Index(name = "idx_cardmarket_price_candidates_card_code", columnList = "card_code"),
                @Index(name = "idx_cardmarket_price_candidates_price_guide_created_at",
                        columnList = "price_guide_created_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cardmarket_price_candidates_product_snapshot",
                columnNames = {"product_id", "price_guide_created_at"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardmarketPriceCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "card_code", nullable = false)
    private String cardCode;

    @Column(name = "expansion_id")
    private Long expansionId;

    @Column(name = "metacard_id")
    private Long metacardId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "date_added")
    private String dateAdded;

    @Column(name = "price_guide_version")
    private String priceGuideVersion;

    @Column(
            name = "price_guide_created_at",
            nullable = false,
            columnDefinition = "timestamp with time zone")
    private OffsetDateTime priceGuideCreatedAt;

    @Column(name = "product_catalog_version")
    private String productCatalogVersion;

    @Column(name = "product_catalog_created_at")
    private String productCatalogCreatedAt;

    @Column(name = "average_price", precision = 12, scale = 2)
    private BigDecimal averagePrice;

    @Column(name = "low_price", precision = 12, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "trend_price", precision = 12, scale = 2)
    private BigDecimal trendPrice;

    @Column(name = "average_price_1_day", precision = 12, scale = 2)
    private BigDecimal averagePrice1Day;

    @Column(name = "average_price_7_days", precision = 12, scale = 2)
    private BigDecimal averagePrice7Days;

    @Column(name = "average_price_30_days", precision = 12, scale = 2)
    private BigDecimal averagePrice30Days;

    @Column(name = "foil_average_price", precision = 12, scale = 2)
    private BigDecimal foilAveragePrice;

    @Column(name = "foil_low_price", precision = 12, scale = 2)
    private BigDecimal foilLowPrice;

    @Column(name = "foil_trend_price", precision = 12, scale = 2)
    private BigDecimal foilTrendPrice;

    @Column(name = "foil_average_price_1_day", precision = 12, scale = 2)
    private BigDecimal foilAveragePrice1Day;

    @Column(name = "foil_average_price_7_days", precision = 12, scale = 2)
    private BigDecimal foilAveragePrice7Days;

    @Column(name = "foil_average_price_30_days", precision = 12, scale = 2)
    private BigDecimal foilAveragePrice30Days;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;
}
