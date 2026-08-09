package pl.janda.onepiecetcg.matchups.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderRepository;
import pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa.MatchupLeaderJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringMatchupLeaderRepository implements MatchupLeaderRepository {

    private final MatchupLeaderJpaRepository jpaRepository;

    @Override
    public List<MatchupLeader> findAllOrderByPopularityDesc() {
        return jpaRepository.findAllByOrderByPopularityDesc();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public void saveAll(List<MatchupLeader> leaders) {
        jpaRepository.saveAll(leaders);
    }
}
