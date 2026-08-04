package pl.janda.onepiecetcg.deckbuilder.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeck;

interface SharedDeckJpaRepository extends JpaRepository<SharedDeck, String> {
}
