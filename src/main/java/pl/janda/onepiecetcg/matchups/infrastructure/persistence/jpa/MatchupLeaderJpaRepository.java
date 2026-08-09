package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;

import java.util.List;

public interface MatchupLeaderJpaRepository extends JpaRepository<MatchupLeader, String> {

    List<MatchupLeader> findAllByOrderByPopularityDesc();
}
