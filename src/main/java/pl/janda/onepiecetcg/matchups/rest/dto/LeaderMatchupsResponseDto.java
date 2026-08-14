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
public class LeaderMatchupsResponseDto {
    private SnapshotDto snapshot;
    private LeaderStatDto leader;
    private List<MatchupDto> matchups;
}
