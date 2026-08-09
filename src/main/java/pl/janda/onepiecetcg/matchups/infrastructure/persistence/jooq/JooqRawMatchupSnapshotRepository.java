package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.RawMatchupSnapshot;
import pl.janda.onepiecetcg.matchups.application.repository.RawMatchupSnapshotRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JooqRawMatchupSnapshotRepository implements RawMatchupSnapshotRepository {

    private final DSLContext dsl;

    @Override
    public Optional<RawMatchupSnapshot> findLatest() {
        return dsl.fetch("""
                        SELECT id, dataset, total_matches, scraped_at
                        FROM tcgmatchmaking_matchup_snapshots
                        ORDER BY scraped_at DESC, id DESC
                        LIMIT 1
                        """)
                .stream()
                .map(record -> RawMatchupSnapshot.builder()
                        .id(record.get("id", Long.class))
                        .dataset(record.get("dataset", String.class))
                        .totalMatches(record.get("total_matches", Long.class))
                        .scrapedAt(record.get("scraped_at", OffsetDateTime.class))
                        .build())
                .findFirst();
    }
}
