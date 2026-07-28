package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;

/**
 * Plain Spring Data JPA repository for simple CRUD / derived queries on SetCard.
 * Complex/read-heavy queries are handled by {@link JooqSetCardQueryAdapter} instead (see CLAUDE.md §3).
 */
interface SetCardJpaRepository extends JpaRepository<SetCard, Long> {

    List<SetCard> findByCardSetId(String cardSetId);

    @Query("select distinct c.cardSetId from SetCard c where c.cardSetId is not null order by c.cardSetId")
    List<String> findDistinctCardSetIds();
}
