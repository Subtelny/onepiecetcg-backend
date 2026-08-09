package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.RawLeaderStat;
import pl.janda.onepiecetcg.matchups.application.repository.RawLeaderStatRepository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqRawLeaderStatRepository implements RawLeaderStatRepository {

    private final DSLContext dsl;

    @Override
    public List<RawLeaderStat> findBySnapshotId(Long snapshotId) {
        return dsl.fetch("""
                        SELECT snapshot_id, leader, wins, losses, number_of_matches, win_rate, popularity
                        FROM (
                            SELECT snapshot_id, leader, wins, losses, number_of_matches, win_rate, popularity,
                                   ROW_NUMBER() OVER (
                                       PARTITION BY snapshot_id, leader
                                       ORDER BY number_of_matches DESC, leader_group_index ASC
                                   ) AS leader_rank
                            FROM tcgmatchmaking_leader_stats
                            WHERE snapshot_id = ?
                        ) ranked_leader_stats
                        WHERE leader_rank = 1
                        """, snapshotId)
                .map(record -> RawLeaderStat.builder()
                        .snapshotId(record.get("snapshot_id", Long.class))
                        .leader(record.get("leader", String.class))
                        .wins(record.get("wins", Long.class))
                        .losses(record.get("losses", Long.class))
                        .numberOfMatches(record.get("number_of_matches", Long.class))
                        .winRate(record.get("win_rate", BigDecimal.class))
                        .popularity(record.get("popularity", BigDecimal.class))
                        .build());
    }
}
