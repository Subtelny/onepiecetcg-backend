package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionValue;

/**
 * Plain Spring Data JPA repository for simple CRUD on CardFilterOptionValue.
 * The read-heavy refresh computation is handled by {@link JooqCardFilterOptionQueryAdapter} instead (see CLAUDE.md §3).
 */
interface CardFilterOptionJpaRepository extends JpaRepository<CardFilterOptionValue, Long> {
}
