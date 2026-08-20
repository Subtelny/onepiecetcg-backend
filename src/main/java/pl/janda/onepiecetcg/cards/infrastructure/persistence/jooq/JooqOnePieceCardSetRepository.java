package pl.janda.onepiecetcg.cards.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCardSet;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardSetRepository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqOnePieceCardSetRepository implements OnePieceCardSetRepository {

    private final DSLContext dsl;

    @Override
    public List<OnePieceCardSet> findAll() {
        return dsl.fetch("""
                        WITH catalog_sets AS (
                            SELECT set_id,
                                   label,
                                   TRUE AS released,
                                   NULL::date AS release_date
                            FROM onepiece_card_sets
                        
                            UNION ALL
                        
                            SELECT leaked.set_id,
                                   leaked.label,
                                   FALSE AS released,
                                   leaked.release_date
                            FROM cardkaizoku_card_sets leaked
                            WHERE leaked.release_date > CURRENT_DATE
                              AND NOT EXISTS (
                                  SELECT 1
                                  FROM onepiece_card_sets official
                                  WHERE official.set_id = leaked.set_id
                              )
                        )
                        SELECT set_id, label, released, release_date
                        FROM catalog_sets
                        ORDER BY set_id
                        """)
                .map(record -> OnePieceCardSet.builder()
                        .setId(record.get("set_id", String.class))
                        .label(record.get("label", String.class))
                        .released(Boolean.TRUE.equals(record.get("released", Boolean.class)))
                        .releaseDate(record.get("release_date", LocalDate.class))
                        .build());
    }
}
