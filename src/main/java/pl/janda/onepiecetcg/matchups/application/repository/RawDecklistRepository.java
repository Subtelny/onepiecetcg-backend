package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.RawDecklist;

import java.util.List;

public interface RawDecklistRepository {

    List<RawDecklist> findBySnapshotId(Long snapshotId);
}
