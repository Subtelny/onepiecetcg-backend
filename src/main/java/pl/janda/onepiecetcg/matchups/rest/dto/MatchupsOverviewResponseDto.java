package pl.janda.onepiecetcg.matchups.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupsOverviewResponseDto {
    private SnapshotDto snapshot;
    private List<LeaderSummaryDto> leaders;
    private List<MatchupDto> topMatchups;
}
