package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;

import java.util.List;
import java.util.Optional;

public interface MatchupSnapshotInfoRepository {

    Optional<MatchupSnapshotInfo> findLatest();

    Optional<MatchupSnapshotInfo> findByDataset(String dataset);

    List<MatchupSnapshotInfo> findAllOrderByScrapedAtDesc();

    void deleteByDataset(String dataset);

    void save(MatchupSnapshotInfo snapshotInfo);
}
