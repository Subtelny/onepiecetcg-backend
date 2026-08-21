package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.RawMatchupSnapshot;

import java.util.List;

public interface RawMatchupSnapshotRepository {

    List<RawMatchupSnapshot> findLatestPerDataset();
}
