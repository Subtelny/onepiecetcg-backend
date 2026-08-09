package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;

public interface MatchupSnapshotInfoJpaRepository extends JpaRepository<MatchupSnapshotInfo, Long> {
}
