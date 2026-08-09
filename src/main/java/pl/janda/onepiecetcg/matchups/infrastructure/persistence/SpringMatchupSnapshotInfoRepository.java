package pl.janda.onepiecetcg.matchups.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupSnapshotInfoRepository;
import pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa.MatchupSnapshotInfoJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpringMatchupSnapshotInfoRepository implements MatchupSnapshotInfoRepository {

    private final MatchupSnapshotInfoJpaRepository jpaRepository;

    @Override
    public Optional<MatchupSnapshotInfo> findCurrent() {
        return jpaRepository.findAll().stream().findFirst();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public void save(MatchupSnapshotInfo snapshotInfo) {
        jpaRepository.save(snapshotInfo);
    }
}
