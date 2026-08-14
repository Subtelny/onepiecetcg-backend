package pl.janda.onepiecetcg.matchups.application.model;

import java.util.List;

public record LeaderMatchups(
        MatchupSnapshotInfo snapshot,
        MatchupLeader leader,
        List<MatchupLeaderCard> leaderCards,
        List<MatchupPair> matchups
) {
}
