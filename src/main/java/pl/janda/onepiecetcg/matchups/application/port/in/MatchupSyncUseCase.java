package pl.janda.onepiecetcg.matchups.application.port.in;

public interface MatchupSyncUseCase {

    boolean syncMatchups();

    boolean recalculateMatchups(String dataset);
}
