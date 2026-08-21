package pl.janda.onepiecetcg.matchups.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupSnapshotInfoRepository;
import pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa.MatchupSnapshotInfoJpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpringMatchupSnapshotInfoRepository implements MatchupSnapshotInfoRepository {

    private final MatchupSnapshotInfoJpaRepository jpaRepository;

    @Override
    public Optional<MatchupSnapshotInfo> findLatest() {
        return jpaRepository.findFirstByOrderByScrapedAtDescIdDesc();
    }

    @Override
    public Optional<MatchupSnapshotInfo> findByDataset(String dataset) {
        return jpaRepository.findFirstByDatasetIgnoreCaseOrderByScrapedAtDescIdDesc(dataset);
    }

    @Override
    public List<MatchupSnapshotInfo> findAllOrderByScrapedAtDesc() {
        return jpaRepository.findAllByOrderByScrapedAtDescIdDesc();
    }

    @Override
    public void deleteByDataset(String dataset) {
        jpaRepository.deleteAllByDataset(dataset);
    }

    @Override
    public void save(MatchupSnapshotInfo snapshotInfo) {
        jpaRepository.save(snapshotInfo);
    }
}
