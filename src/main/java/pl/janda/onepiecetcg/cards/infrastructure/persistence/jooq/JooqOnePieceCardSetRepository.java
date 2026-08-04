package pl.janda.onepiecetcg.cards.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCardSet;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardSetRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqOnePieceCardSetRepository implements OnePieceCardSetRepository {

    private final DSLContext dsl;

    @Override
    public List<OnePieceCardSet> findAll() {
        return dsl.fetch("""
                        SELECT set_id, label
                        FROM onepiece_card_sets
                        ORDER BY set_id
                        """)
                .map(record -> OnePieceCardSet.builder()
                        .setId(record.get("set_id", String.class))
                        .label(record.get("label", String.class))
                        .build());
    }
}
