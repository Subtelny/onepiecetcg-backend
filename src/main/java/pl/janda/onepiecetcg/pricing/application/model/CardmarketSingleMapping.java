package pl.janda.onepiecetcg.pricing.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "cardmarket_single_mappings",
        indexes = {
                @Index(name = "idx_cardmarket_single_mappings_card_code", columnList = "card_code"),
                @Index(name = "idx_cardmarket_single_mappings_expansion", columnList = "expansion_id")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cardmarket_single_mappings_price_reference",
                columnNames = "price_reference"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardmarketSingleMapping {

    @Id
    @Column(name = "cardmarket_product_id")
    private Long cardmarketProductId;

    @Column(name = "price_reference", nullable = false)
    private String priceReference;

    @Column(name = "card_code", nullable = false)
    private String cardCode;

    @Column(name = "expansion_id", nullable = false)
    private Long expansionId;

    @Column(name = "local_variant", nullable = false)
    private Integer localVariant;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private CardmarketSingleMatchType matchType;

    @Column(name = "confidence", nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "last_matched_at", nullable = false)
    private LocalDateTime lastMatchedAt;
}
