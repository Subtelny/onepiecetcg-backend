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
public class RawLeaderStat {

    private Long snapshotId;

    private String leader;

    private Long wins;

    private Long losses;

    private Long numberOfMatches;

    private BigDecimal winRate;

    private BigDecimal popularity;
}
