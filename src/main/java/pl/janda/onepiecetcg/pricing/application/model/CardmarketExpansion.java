package pl.janda.onepiecetcg.pricing.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cardmarket_expansion_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardmarketExpansion {

    @Id
    @Column(name = "expansion_id")
    private Long expansionId;

    @Column(name = "release_id")
    private String releaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type")
    private CardmarketExpansionMatchType matchType;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "last_resolved_at", nullable = false)
    private LocalDateTime lastResolvedAt;
}
