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
    public List<MatchupLeaderCard> findAllOrderByLeaderAndCategoryAndInclusionRate() {
        return jpaRepository.findAllByOrderByLeaderCodeAscCategoryAscInclusionRateDescCardCodeAsc();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public void saveAll(List<MatchupLeaderCard> cards) {
        jpaRepository.saveAll(cards);
    }
}
