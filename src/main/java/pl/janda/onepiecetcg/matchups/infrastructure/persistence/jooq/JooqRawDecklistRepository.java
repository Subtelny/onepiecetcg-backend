package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.RawDecklist;
import pl.janda.onepiecetcg.matchups.application.repository.RawDecklistRepository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqRawDecklistRepository implements RawDecklistRepository {

    private final DSLContext dsl;

    @Override
    public List<RawDecklist> findBySnapshotId(Long snapshotId) {
        return dsl.fetch("""
                        SELECT snapshot_id, leader, deck::text AS deck, games, win_rate
                        FROM tcgmatchmaking_decklists
                        WHERE snapshot_id = ?
                          AND games > 0
                        """, snapshotId)
                .map(record -> RawDecklist.builder()
                        .snapshotId(record.get("snapshot_id", Long.class))
                        .leader(record.get("leader", String.class))
                        .deck(record.get("deck", String.class))
                        .games(record.get("games", Long.class))
                        .winRate(record.get("win_rate", BigDecimal.class))
                        .build());
    }
}
