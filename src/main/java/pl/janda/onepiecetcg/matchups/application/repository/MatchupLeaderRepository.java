package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;

import java.util.List;
import java.util.Optional;

public interface MatchupLeaderRepository {

    List<MatchupLeader> findAllOrderByPopularityDesc(String dataset);

    Optional<MatchupLeader> findByCode(String dataset, String code);

    boolean hasAnyRepresentativeDeck(String dataset);

    void deleteByDataset(String dataset);

    void saveAll(List<MatchupLeader> leaders);
}
