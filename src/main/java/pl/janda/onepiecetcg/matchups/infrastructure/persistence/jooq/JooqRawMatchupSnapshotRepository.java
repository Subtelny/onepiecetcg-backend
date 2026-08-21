package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.RawMatchupSnapshot;
import pl.janda.onepiecetcg.matchups.application.repository.RawMatchupSnapshotRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqRawMatchupSnapshotRepository implements RawMatchupSnapshotRepository {

    private final DSLContext dsl;

    @Override
    public List<RawMatchupSnapshot> findLatestPerDataset() {
        return dsl.fetch("""
                        SELECT id, dataset, total_matches, scraped_at
                        FROM (
                            SELECT DISTINCT ON (LOWER(dataset))
                                   id, dataset, total_matches, scraped_at
                            FROM tcgmatchmaking_matchup_snapshots
                            ORDER BY LOWER(dataset), scraped_at DESC, id DESC
                        ) latest
                        ORDER BY scraped_at DESC, id DESC
                        """)
                .stream()
                .map(record -> RawMatchupSnapshot.builder()
                        .id(record.get("id", Long.class))
                        .dataset(record.get("dataset", String.class))
                        .totalMatches(record.get("total_matches", Long.class))
                        .scrapedAt(record.get("scraped_at", OffsetDateTime.class))
                        .build())
                .toList();
    }
}
