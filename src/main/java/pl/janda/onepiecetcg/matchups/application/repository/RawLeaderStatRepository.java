package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.RawLeaderStat;

import java.util.List;

public interface RawLeaderStatRepository {

    List<RawLeaderStat> findBySnapshotId(Long snapshotId);
}
