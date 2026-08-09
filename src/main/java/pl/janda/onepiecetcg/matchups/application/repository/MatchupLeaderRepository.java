package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;

import java.util.List;

public interface MatchupLeaderRepository {

    List<MatchupLeader> findAllOrderByPopularityDesc();

    void deleteAll();

    void saveAll(List<MatchupLeader> leaders);
}
