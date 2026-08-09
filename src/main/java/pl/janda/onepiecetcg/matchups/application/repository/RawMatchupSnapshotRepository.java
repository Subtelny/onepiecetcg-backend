package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.RawMatchupSnapshot;

import java.util.Optional;

public interface RawMatchupSnapshotRepository {

    Optional<RawMatchupSnapshot> findLatest();
}
