package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPairId;

import java.util.List;
import java.util.Set;

public interface MatchupPairJpaRepository extends JpaRepository<MatchupPair, MatchupPairId> {

    List<MatchupPair> findAllByDataset(String dataset);

    List<MatchupPair> findAllByDatasetAndLeaderCode(String dataset, String leaderCode);

    List<MatchupPair> findAllByDatasetAndLeaderCodeInAndOpponentCodeIn(
            String dataset, Set<String> leaderCodes, Set<String> opponentCodes);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MatchupPair pair where lower(pair.dataset) = lower(:dataset)")
    void deleteAllByDataset(@Param("dataset") String dataset);
}
