package pl.janda.onepiecetcg.deckbuilder.application.repository;

import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeck;

import java.util.Optional;

public interface SharedDeckRepository {

    SharedDeck save(SharedDeck sharedDeck);

    Optional<SharedDeck> findByShareCode(String shareCode);

    boolean existsByShareCode(String shareCode);
}
