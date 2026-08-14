package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCard;

import java.util.List;

public interface MatchupLeaderCardRepository {

    List<MatchupLeaderCard> findAllOrderByLeaderAndCategoryAndInclusionRate();

    List<MatchupLeaderCard> findByLeaderCode(String leaderCode);

    void deleteAll();

    void saveAll(List<MatchupLeaderCard> cards);
}
