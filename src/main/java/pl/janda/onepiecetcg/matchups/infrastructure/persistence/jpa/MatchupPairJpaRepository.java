package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPairId;

import java.util.List;
import java.util.Set;

public interface MatchupPairJpaRepository extends JpaRepository<MatchupPair, MatchupPairId> {

    List<MatchupPair> findAllByLeaderCode(String leaderCode);

    List<MatchupPair> findAllByLeaderCodeInAndOpponentCodeIn(Set<String> leaderCodes, Set<String> opponentCodes);
}
