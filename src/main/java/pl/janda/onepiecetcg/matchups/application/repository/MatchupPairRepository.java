package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;

import java.util.List;

public interface MatchupPairRepository {

    List<MatchupPair> findAll();

    void deleteAll();

    void saveAll(List<MatchupPair> pairs);
}
