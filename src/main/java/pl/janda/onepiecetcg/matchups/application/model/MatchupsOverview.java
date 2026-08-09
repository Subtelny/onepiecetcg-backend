package pl.janda.onepiecetcg.matchups.application.model;

import java.util.List;

public record MatchupsOverview(
        MatchupSnapshotInfo snapshot,
        List<MatchupLeader> leaders,
        List<MatchupPair> matchups,
        List<MatchupPair> topMatchups
) {
}
