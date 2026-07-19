package pl.janda.onepiecetcg.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.application.model.SetCard;

import java.util.List;

/**
 * Plain Spring Data JPA repository for simple CRUD / derived queries on SetCard.
 * Complex/read-heavy queries are handled by {@link JooqSetCardQueryAdapter} instead (see CLAUDE.md §3).
 */
interface SetCardJpaRepository extends JpaRepository<SetCard, Long> {

    List<SetCard> findByCardSetId(String cardSetId);
}
