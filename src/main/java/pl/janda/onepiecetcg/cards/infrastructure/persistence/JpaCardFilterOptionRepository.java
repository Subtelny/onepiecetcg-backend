package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionValue;
import pl.janda.onepiecetcg.cards.application.repository.CardFilterOptionRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaCardFilterOptionRepository implements CardFilterOptionRepository {

    private final CardFilterOptionJpaRepository jpaRepository;

    private final JooqCardFilterOptionQueryAdapter jooqQueryAdapter;

    @Override
    public List<CardFilterOptionValue> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public <S extends CardFilterOptionValue> List<S> saveAll(Iterable<S> values) {
        return jpaRepository.saveAll(values);
    }

    @Override
    public void refresh() {
        jooqQueryAdapter.refresh();
    }
}
