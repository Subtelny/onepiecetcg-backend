package pl.janda.onepiecetcg.matchups.application.port.in;

import pl.janda.onepiecetcg.matchups.application.model.MatchupsOverview;

public interface MatchupsQueryUseCase {

    MatchupsOverview getMatchups();
}
