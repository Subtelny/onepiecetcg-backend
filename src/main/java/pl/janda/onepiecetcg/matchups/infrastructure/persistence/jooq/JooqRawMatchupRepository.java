package pl.janda.onepiecetcg.matchups.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.matchups.application.model.RawMatchup;
import pl.janda.onepiecetcg.matchups.application.repository.RawMatchupRepository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JooqRawMatchupRepository implements RawMatchupRepository {

    private final DSLContext dsl;

    @Override
    public List<RawMatchup> findBySnapshotId(Long snapshotId) {
        return dsl.fetch("""
                        SELECT snapshot_id, leader, opponent, wins, losses, games,
                               win_rate, first_win_rate, second_win_rate, first_games, second_games
                        FROM tcgmatchmaking_matchups
                        WHERE snapshot_id = ?
                        """, snapshotId)
                .map(record -> RawMatchup.builder()
                        .snapshotId(record.get("snapshot_id", Long.class))
                        .leader(record.get("leader", String.class))
                        .opponent(record.get("opponent", String.class))
                        .wins(record.get("wins", Long.class))
                        .losses(record.get("losses", Long.class))
                        .games(record.get("games", Long.class))
                        .winRate(record.get("win_rate", BigDecimal.class))
                        .firstWinRate(record.get("first_win_rate", BigDecimal.class))
                        .secondWinRate(record.get("second_win_rate", BigDecimal.class))
                        .firstGames(record.get("first_games", Long.class))
                        .secondGames(record.get("second_games", Long.class))
                        .build());
    }
}
