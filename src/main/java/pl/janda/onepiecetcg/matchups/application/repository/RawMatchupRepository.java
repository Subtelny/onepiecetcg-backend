package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.RawMatchup;

import java.util.List;

public interface RawMatchupRepository {

    List<RawMatchup> findBySnapshotId(Long snapshotId);
}
