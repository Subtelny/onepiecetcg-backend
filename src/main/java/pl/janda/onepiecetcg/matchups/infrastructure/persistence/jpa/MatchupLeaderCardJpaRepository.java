package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCard;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCardId;

import java.util.List;

public interface MatchupLeaderCardJpaRepository extends JpaRepository<MatchupLeaderCard, MatchupLeaderCardId> {

    List<MatchupLeaderCard> findAllByOrderByLeaderCodeAscCategoryAscInclusionRateDescCardCodeAsc();
}
