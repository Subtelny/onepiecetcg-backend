package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCard;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCardId;

import java.util.List;

public interface MatchupLeaderCardJpaRepository extends JpaRepository<MatchupLeaderCard, MatchupLeaderCardId> {

    List<MatchupLeaderCard> findAllByDatasetOrderByLeaderCodeAscCategoryAscInclusionRateDescCardCodeAsc(
            String dataset);

    List<MatchupLeaderCard> findAllByDatasetAndLeaderCodeOrderByCategoryAscInclusionRateDescCardCodeAsc(
            String dataset, String leaderCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MatchupLeaderCard card where lower(card.dataset) = lower(:dataset)")
    void deleteAllByDataset(@Param("dataset") String dataset);
}
