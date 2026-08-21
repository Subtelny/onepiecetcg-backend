package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;

import java.util.List;
import java.util.Optional;

public interface MatchupSnapshotInfoJpaRepository extends JpaRepository<MatchupSnapshotInfo, Long> {

    Optional<MatchupSnapshotInfo> findFirstByOrderByScrapedAtDescIdDesc();

    Optional<MatchupSnapshotInfo> findFirstByDatasetIgnoreCaseOrderByScrapedAtDescIdDesc(String dataset);

    List<MatchupSnapshotInfo> findAllByOrderByScrapedAtDescIdDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MatchupSnapshotInfo snapshot where lower(snapshot.dataset) = lower(:dataset)")
    void deleteAllByDataset(@Param("dataset") String dataset);
}
