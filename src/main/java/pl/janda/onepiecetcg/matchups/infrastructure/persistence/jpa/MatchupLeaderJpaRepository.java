package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderId;

import java.util.List;
import java.util.Optional;

public interface MatchupLeaderJpaRepository extends JpaRepository<MatchupLeader, MatchupLeaderId> {

    List<MatchupLeader> findAllByDatasetOrderByPopularityDesc(String dataset);

    Optional<MatchupLeader> findByDatasetAndCardCode(String dataset, String cardCode);

    boolean existsByDatasetAndTopDeckGamesIsNotNull(String dataset);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MatchupLeader leader where lower(leader.dataset) = lower(:dataset)")
    void deleteAllByDataset(@Param("dataset") String dataset);
}
