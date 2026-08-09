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
public class MatchupsResponseDto {
    private SnapshotDto snapshot;
    private List<LeaderStatDto> leaders;
    private List<MatchupDto> matchups;
    private List<MatchupDto> topMatchups;
}
