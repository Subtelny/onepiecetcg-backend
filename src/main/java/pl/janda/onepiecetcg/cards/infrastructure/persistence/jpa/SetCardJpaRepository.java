package pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;
import java.util.Optional;


public interface SetCardJpaRepository extends JpaRepository<SetCard, Long> {

    List<SetCard> findByCardSetId(String cardSetId);

    List<SetCard> findByCardSetIdIn(List<String> cardSetIds);

    Optional<SetCard> findByCardSetIdAndVariantIndex(String cardSetId, String variantIndex);

    List<SetCard> findByCardSetIdInAndVariantIndex(List<String> cardSetIds, String variantIndex);

    @Query("select distinct c.cardSetId from SetCard c where c.cardSetId is not null order by c.cardSetId")
    List<String> findDistinctCardSetIds();
}
