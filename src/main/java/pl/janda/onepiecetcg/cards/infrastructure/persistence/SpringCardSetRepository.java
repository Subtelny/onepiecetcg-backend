package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardSet;
import pl.janda.onepiecetcg.cards.application.repository.CardSetRepository;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa.CardSetJpaRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SpringCardSetRepository implements CardSetRepository {

    private final CardSetJpaRepository jpaRepository;

    @Override
    public List<CardSet> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<CardSet> findById(String setId) {
        return jpaRepository.findById(setId);
    }

    @Override
    public void saveAll(List<CardSet> cardSets) {
        jpaRepository.saveAll(cardSets);
    }
}
