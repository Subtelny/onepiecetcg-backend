package pl.janda.onepiecetcg.matchups.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCard;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderCardRepository;
import pl.janda.onepiecetcg.matchups.infrastructure.persistence.jpa.MatchupLeaderCardJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringMatchupLeaderCardRepository implements MatchupLeaderCardRepository {

    private final MatchupLeaderCardJpaRepository jpaRepository;

    @Override
    public List<MatchupLeaderCard> findAllOrderByLeaderAndCategoryAndInclusionRate(String dataset) {
        return jpaRepository.findAllByDatasetOrderByLeaderCodeAscCategoryAscInclusionRateDescCardCodeAsc(dataset);
    }

    @Override
    public List<MatchupLeaderCard> findByLeaderCode(String dataset, String leaderCode) {
        return jpaRepository.findAllByDatasetAndLeaderCodeOrderByCategoryAscInclusionRateDescCardCodeAsc(
                dataset, leaderCode);
    }

    @Override
    public void deleteByDataset(String dataset) {
        jpaRepository.deleteAllByDataset(dataset);
    }

    @Override
    public void saveAll(List<MatchupLeaderCard> cards) {
        jpaRepository.saveAll(cards);
    }
}
