package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;

import java.util.List;
import java.util.Set;

public interface MatchupPairRepository {

    List<MatchupPair> findAll();

    List<MatchupPair> findByLeaderCode(String leaderCode);

    List<MatchupPair> findByLeaderCodes(Set<String> leaderCodes);

    void deleteAll();

    void saveAll(List<MatchupPair> pairs);
}
