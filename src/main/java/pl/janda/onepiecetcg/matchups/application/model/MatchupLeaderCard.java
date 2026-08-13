package pl.janda.onepiecetcg.matchups.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "matchup_leader_cards")
@IdClass(MatchupLeaderCardId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupLeaderCard {

    @Id
    @Column(name = "leader_code", nullable = false)
    private String leaderCode;

    @Id
    @Column(name = "card_code", nullable = false)
    private String cardCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private MatchupLeaderCardCategory category;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "card_type")
    private String cardType;

    @Column(name = "card_cost")
    private Integer cost;

    @Column(name = "card_power")
    private Integer power;

    @Column(name = "counter_amount")
    private Integer counter;

    @Column(name = "card_text", columnDefinition = "TEXT")
    private String effect;

    @Column(name = "inclusion_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal inclusionRate;

    @Column(name = "typical_copies", nullable = false, precision = 3, scale = 1)
    private BigDecimal typicalCopies;
}
