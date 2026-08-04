package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardFilterOptionValue;
import pl.janda.onepiecetcg.cards.application.repository.CardFilterOptionRepository;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jooq.JooqCardFilterOptionQueryAdapter;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa.CardFilterOptionJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringCardFilterOptionRepository implements CardFilterOptionRepository {

    private final CardFilterOptionJpaRepository jpaRepository;

    private final JooqCardFilterOptionQueryAdapter jooqQueryAdapter;

    @Override
    public List<CardFilterOptionValue> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void refresh() {
        jooqQueryAdapter.refresh();
    }
}
