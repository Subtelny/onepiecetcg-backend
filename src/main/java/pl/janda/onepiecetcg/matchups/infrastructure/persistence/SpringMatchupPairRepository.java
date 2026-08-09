package pl.janda.onepiecetcg.matchups.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupPairRepository;
import pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa.MatchupPairJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringMatchupPairRepository implements MatchupPairRepository {

    private final MatchupPairJpaRepository jpaRepository;

    @Override
    public List<MatchupPair> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public void saveAll(List<MatchupPair> pairs) {
        jpaRepository.saveAll(pairs);
    }
}
