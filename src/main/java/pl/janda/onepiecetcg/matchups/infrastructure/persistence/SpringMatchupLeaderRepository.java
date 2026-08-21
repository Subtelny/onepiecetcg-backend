package pl.janda.onepiecetcg.matchups.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderRepository;
import pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa.MatchupLeaderJpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpringMatchupLeaderRepository implements MatchupLeaderRepository {

    private final MatchupLeaderJpaRepository jpaRepository;

    @Override
    public List<MatchupLeader> findAllOrderByPopularityDesc(String dataset) {
        return jpaRepository.findAllByDatasetOrderByPopularityDesc(dataset);
    }

    @Override
    public Optional<MatchupLeader> findByCode(String dataset, String code) {
        return jpaRepository.findByDatasetAndCardCode(dataset, code);
    }

    @Override
    public boolean hasAnyRepresentativeDeck(String dataset) {
        return jpaRepository.existsByDatasetAndTopDeckGamesIsNotNull(dataset);
    }

    @Override
    public void deleteByDataset(String dataset) {
        jpaRepository.deleteAllByDataset(dataset);
    }

    @Override
    public void saveAll(List<MatchupLeader> leaders) {
        jpaRepository.saveAll(leaders);
    }
}
