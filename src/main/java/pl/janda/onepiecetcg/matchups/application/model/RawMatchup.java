package pl.janda.onepiecetcg.matchups.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMatchup {

    private Long snapshotId;

    private String leader;

    private String opponent;

    private Long wins;

    private Long losses;

    private Long games;

    private BigDecimal winRate;

    private BigDecimal firstWinRate;

    private BigDecimal secondWinRate;

    private Long firstGames;

    private Long secondGames;
}
