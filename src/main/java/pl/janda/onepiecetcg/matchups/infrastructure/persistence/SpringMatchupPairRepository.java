package pl.janda.onepiecetcg.matchups.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupPairRepository;
import pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa.MatchupPairJpaRepository;

import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class SpringMatchupPairRepository implements MatchupPairRepository {

    private final MatchupPairJpaRepository jpaRepository;

    @Override
    public List<MatchupPair> findAll(String dataset) {
        return jpaRepository.findAllByDataset(dataset);
    }

    @Override
    public List<MatchupPair> findByLeaderCode(String dataset, String leaderCode) {
        return jpaRepository.findAllByDatasetAndLeaderCode(dataset, leaderCode);
    }

    @Override
    public List<MatchupPair> findByLeaderCodes(String dataset, Set<String> leaderCodes) {
        return jpaRepository.findAllByDatasetAndLeaderCodeInAndOpponentCodeIn(dataset, leaderCodes, leaderCodes);
    }

    @Override
    public void deleteByDataset(String dataset) {
        jpaRepository.deleteAllByDataset(dataset);
    }

    @Override
    public void saveAll(List<MatchupPair> pairs) {
        jpaRepository.saveAll(pairs);
    }
}
