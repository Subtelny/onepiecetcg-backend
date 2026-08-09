package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;

import java.util.Optional;

public interface MatchupSnapshotInfoRepository {

    Optional<MatchupSnapshotInfo> findCurrent();

    void deleteAll();

    void save(MatchupSnapshotInfo snapshotInfo);
}
