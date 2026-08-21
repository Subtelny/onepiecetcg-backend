package pl.janda.onepiecetcg.matchups.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "matchup_leaders")
@IdClass(MatchupLeaderId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupLeader {

    @Id
    @Column(name = "dataset", nullable = false, columnDefinition = "varchar(255) default ''")
    private String dataset;

    @Id
    @Column(name = "card_code")
    private String cardCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "colors")
    private String colors;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "popularity", nullable = false)
    private BigDecimal popularity;

    @Column(name = "matches", nullable = false)
    private Long matches;

    @Column(name = "win_rate", nullable = false)
    private BigDecimal winRate;

    @Column(name = "profile_decklists")
    private Integer profileDecklists;

    @Column(name = "top_deck_games")
    private Long topDeckGames;

    @Column(name = "top_deck_win_rate")
    private BigDecimal topDeckWinRate;
}
