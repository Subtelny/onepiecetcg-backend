package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.CardErrata;
import pl.janda.onepiecetcg.cards.application.repository.CardErrataRepository;
import pl.janda.onepiecetcg.cards.infrastructure.persistence.jpa.CardErrataJpaRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpringCardErrataRepository implements CardErrataRepository {

    private final CardErrataJpaRepository jpaRepository;

    @Override
    public List<CardErrata> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAllInBatch();
    }

    @Override
    public void saveAll(List<CardErrata> errata) {
        jpaRepository.saveAll(errata);
    }

    @Override
    public List<CardErrata> findByCardCodeIn(List<String> cardCodes) {
        return jpaRepository.findByCardCodeIn(cardCodes);
    }
}
