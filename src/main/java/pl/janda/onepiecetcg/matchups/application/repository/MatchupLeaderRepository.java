package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;

import java.util.List;
import java.util.Optional;

public interface MatchupLeaderRepository {

    List<MatchupLeader> findAllOrderByPopularityDesc();

    Optional<MatchupLeader> findByCode(String code);

    boolean hasAnyRepresentativeDeck();

    void deleteAll();

    void saveAll(List<MatchupLeader> leaders);
}
