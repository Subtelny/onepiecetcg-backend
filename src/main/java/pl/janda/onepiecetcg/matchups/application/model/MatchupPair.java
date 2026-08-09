package pl.janda.onepiecetcg.matchups.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "matchup_pairs")
@IdClass(MatchupPairId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupPair {

    @Id
    @Column(name = "leader_code")
    private String leaderCode;

    @Id
    @Column(name = "opponent_code")
    private String opponentCode;

    @Column(name = "games", nullable = false)
    private Long games;

    @Column(name = "win_rate", nullable = false)
    private BigDecimal winRate;

    @Column(name = "first_win_rate")
    private BigDecimal firstWinRate;

    @Column(name = "second_win_rate")
    private BigDecimal secondWinRate;

    @Column(name = "first_games", nullable = false)
    private Long firstGames;

    @Column(name = "second_games", nullable = false)
    private Long secondGames;
}
