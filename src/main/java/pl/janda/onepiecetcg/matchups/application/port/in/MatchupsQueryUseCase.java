package pl.janda.onepiecetcg.matchups.application.port.in;

import pl.janda.onepiecetcg.matchups.application.model.LeaderMatchups;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;
import pl.janda.onepiecetcg.matchups.application.model.MatchupsOverview;
import pl.janda.onepiecetcg.matchups.application.model.MatchupsSummary;

import java.util.List;
import java.util.Optional;

public interface MatchupsQueryUseCase {

    MatchupsOverview getMatchups(String dataset);

    MatchupsSummary getOverview(String dataset);

    Optional<LeaderMatchups> getLeaderMatchups(String dataset, String leaderCode);

    List<MatchupSnapshotInfo> getAvailableSnapshots();
}
