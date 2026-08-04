package pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;


public interface SetCardJpaRepository extends JpaRepository<SetCard, Long> {

    List<SetCard> findByCardSetId(String cardSetId);

    @Query("select distinct c.cardSetId from SetCard c where c.cardSetId is not null order by c.cardSetId")
    List<String> findDistinctCardSetIds();
}
