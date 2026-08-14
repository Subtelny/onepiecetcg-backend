package pl.janda.onepiecetcg.matchups.application.port.in;

import pl.janda.onepiecetcg.matchups.application.model.LeaderMatchups;
import pl.janda.onepiecetcg.matchups.application.model.MatchupsOverview;
import pl.janda.onepiecetcg.matchups.application.model.MatchupsSummary;

import java.util.Optional;

public interface MatchupsQueryUseCase {

    MatchupsOverview getMatchups();

    MatchupsSummary getOverview();

    Optional<LeaderMatchups> getLeaderMatchups(String leaderCode);
}
