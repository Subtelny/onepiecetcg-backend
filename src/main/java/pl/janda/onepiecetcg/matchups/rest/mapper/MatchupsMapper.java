package pl.janda.onepiecetcg.matchups.rest.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;
import pl.janda.onepiecetcg.matchups.application.model.MatchupsOverview;
import pl.janda.onepiecetcg.matchups.rest.dto.LeaderStatDto;
import pl.janda.onepiecetcg.matchups.rest.dto.MatchupDto;
import pl.janda.onepiecetcg.matchups.rest.dto.MatchupsResponseDto;
import pl.janda.onepiecetcg.matchups.rest.dto.SnapshotDto;

import java.util.Arrays;
import java.util.List;

@Component
public class MatchupsMapper {

    public MatchupsResponseDto toDto(MatchupsOverview overview) {
        return MatchupsResponseDto.builder()
                .snapshot(toSnapshotDto(overview.snapshot()))
                .leaders(overview.leaders().stream().map(this::toLeaderStatDto).toList())
                .matchups(overview.matchups().stream().map(this::toMatchupDto).toList())
                .topMatchups(overview.topMatchups().stream().map(this::toMatchupDto).toList())
                .build();
    }

    private SnapshotDto toSnapshotDto(MatchupSnapshotInfo snapshot) {
        if (snapshot == null) {
            return null;
        }
        return SnapshotDto.builder()
                .dataset(snapshot.getDataset())
                .totalMatches(snapshot.getTotalMatches())
                .scrapedAt(snapshot.getScrapedAt() != null ? snapshot.getScrapedAt().toString() : null)
                .build();
    }

    private LeaderStatDto toLeaderStatDto(MatchupLeader leader) {
        return LeaderStatDto.builder()
                .code(leader.getCardCode())
                .name(leader.getName())
                .colors(parseColors(leader.getColors()))
                .imageUrl(leader.getImageUrl())
                .popularity(leader.getPopularity())
                .matches(leader.getMatches())
                .winRate(leader.getWinRate())
                .build();
    }

    private MatchupDto toMatchupDto(MatchupPair pair) {
        return MatchupDto.builder()
                .leaderCode(pair.getLeaderCode())
                .opponentCode(pair.getOpponentCode())
                .games(pair.getGames())
                .winRate(pair.getWinRate())
                .firstWinRate(pair.getFirstWinRate())
                .secondWinRate(pair.getSecondWinRate())
                .firstGames(pair.getFirstGames())
                .secondGames(pair.getSecondGames())
                .build();
    }

    private List<String> parseColors(String colors) {
        if (colors == null || colors.isBlank()) {
            return List.of();
        }
        return Arrays.stream(colors.split("\\s+"))
                .map(String::toUpperCase)
                .toList();
    }
}
