package pl.janda.onepiecetcg.deckbuilder.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.deckbuilder.application.model.SharedDeck;
import pl.janda.onepiecetcg.deckbuilder.application.repository.SharedDeckRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpringSharedDeckRepository implements SharedDeckRepository {

    private final SharedDeckJpaRepository jpaRepository;

    @Override
    public SharedDeck save(SharedDeck sharedDeck) {
        return jpaRepository.save(sharedDeck);
    }

    @Override
    public Optional<SharedDeck> findByShareCode(String shareCode) {
        return jpaRepository.findById(shareCode);
    }

    @Override
    public boolean existsByShareCode(String shareCode) {
        return jpaRepository.existsById(shareCode);
    }
}
