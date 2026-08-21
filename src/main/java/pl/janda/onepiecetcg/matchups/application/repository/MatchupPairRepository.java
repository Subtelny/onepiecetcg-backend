package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;

import java.util.List;
import java.util.Set;

public interface MatchupPairRepository {

    List<MatchupPair> findAll(String dataset);

    List<MatchupPair> findByLeaderCode(String dataset, String leaderCode);

    List<MatchupPair> findByLeaderCodes(String dataset, Set<String> leaderCodes);

    void deleteByDataset(String dataset);

    void saveAll(List<MatchupPair> pairs);
}
