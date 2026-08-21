package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCard;

import java.util.List;

public interface MatchupLeaderCardRepository {

    List<MatchupLeaderCard> findAllOrderByLeaderAndCategoryAndInclusionRate(String dataset);

    List<MatchupLeaderCard> findByLeaderCode(String dataset, String leaderCode);

    void deleteByDataset(String dataset);

    void saveAll(List<MatchupLeaderCard> cards);
}
