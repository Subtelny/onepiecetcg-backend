package pl.janda.onepiecetcg.matchups.application.repository;

import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCard;

import java.util.List;

public interface MatchupLeaderCardRepository {

    List<MatchupLeaderCard> findAllOrderByLeaderAndCategoryAndInclusionRate();

    void deleteAll();

    void saveAll(List<MatchupLeaderCard> cards);
}
