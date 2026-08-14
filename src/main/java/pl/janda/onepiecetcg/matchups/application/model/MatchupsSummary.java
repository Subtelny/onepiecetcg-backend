package pl.janda.onepiecetcg.matchups.application.model;

import java.util.List;

public record MatchupsSummary(
        MatchupSnapshotInfo snapshot,
        List<MatchupLeader> leaders,
        List<MatchupPair> topMatchups
) {
}
