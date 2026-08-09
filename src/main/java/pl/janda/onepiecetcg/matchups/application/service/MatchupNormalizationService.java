package pl.janda.onepiecetcg.matchups.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.matchups.application.model.NormalizedLeaderStat;
import pl.janda.onepiecetcg.matchups.application.model.NormalizedMatchup;
import pl.janda.onepiecetcg.matchups.application.model.RawLeaderStat;
import pl.janda.onepiecetcg.matchups.application.model.RawMatchup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchupNormalizationService {

    private final LeaderCodeNormalizer leaderCodeNormalizer;

    public List<NormalizedLeaderStat> normalizeAndMergeLeaderStats(List<RawLeaderStat> rawStats) {
        var byCode = rawStats.stream()
                .map(this::withNormalizedCode)
                .filter(pair -> {
                    if (pair.code() == null) {
                        log.warn("Dropping leader stat with unparseable leader '{}'", pair.raw().getLeader());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.groupingBy(NormalizedLeaderStatPair::code,
                        Collectors.mapping(NormalizedLeaderStatPair::raw, Collectors.toList())));

        return byCode.entrySet().stream()
                .map(entry -> {
                    var code = entry.getKey();
                    var group = entry.getValue();
                    if (group.size() > 1) {
                        log.warn("Merging {} leader stat rows into a single leader code '{}'", group.size(), code);
                    }

                    var wins = group.stream().mapToLong(RawLeaderStat::getWins).sum();
                    var losses = group.stream().mapToLong(RawLeaderStat::getLosses).sum();
                    var matches = group.stream().mapToLong(RawLeaderStat::getNumberOfMatches).sum();

                    var winRate = winRate(wins, losses);
                    var popularity = weightedAverage(group, RawLeaderStat::getPopularity, RawLeaderStat::getNumberOfMatches);

                    return new NormalizedLeaderStat(code, matches, winRate, popularity);
                })
                .toList();
    }

    public List<NormalizedMatchup> normalizeAndMergeMatchups(List<RawMatchup> rawMatchups, Set<String> validLeaderCodes) {
        var byPair = rawMatchups.stream()
                .map(this::withNormalizedCodes)
                .filter(pair -> {
                    if (pair.leaderCode() == null || pair.opponentCode() == null) {
                        log.warn("Dropping matchup with unparseable leader/opponent '{}' / '{}'",
                                pair.raw().getLeader(), pair.raw().getOpponent());
                        return false;
                    }
                    if (!validLeaderCodes.contains(pair.leaderCode()) || !validLeaderCodes.contains(pair.opponentCode())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.groupingBy(pair -> pair.leaderCode() + "|" + pair.opponentCode(),
                        Collectors.toList()));

        return byPair.values().stream()
                .map(group -> {
                    var first = group.get(0);
                    if (group.size() > 1) {
                        log.warn("Merging {} matchup rows into a single pair '{}' vs '{}'",
                                group.size(), first.leaderCode(), first.opponentCode());
                    }

                    var rawRows = group.stream().map(NormalizedMatchupPair::raw).toList();
                    var wins = rawRows.stream().mapToLong(RawMatchup::getWins).sum();
                    var losses = rawRows.stream().mapToLong(RawMatchup::getLosses).sum();
                    var games = rawRows.stream().mapToLong(RawMatchup::getGames).sum();
                    var firstGames = rawRows.stream().mapToLong(RawMatchup::getFirstGames).sum();
                    var secondGames = rawRows.stream().mapToLong(RawMatchup::getSecondGames).sum();

                    var winRate = winRate(wins, losses);
                    var firstWinRate = weightedAverage(rawRows, RawMatchup::getFirstWinRate, RawMatchup::getFirstGames);
                    var secondWinRate = weightedAverage(rawRows, RawMatchup::getSecondWinRate, RawMatchup::getSecondGames);

                    return new NormalizedMatchup(first.leaderCode(), first.opponentCode(), games,
                            winRate, firstWinRate, secondWinRate, firstGames, secondGames);
                })
                .toList();
    }

    private NormalizedLeaderStatPair withNormalizedCode(RawLeaderStat raw) {
        return new NormalizedLeaderStatPair(leaderCodeNormalizer.extractCardCode(raw.getLeader()).orElse(null), raw);
    }

    private NormalizedMatchupPair withNormalizedCodes(RawMatchup raw) {
        return new NormalizedMatchupPair(
                leaderCodeNormalizer.extractCardCode(raw.getLeader()).orElse(null),
                leaderCodeNormalizer.extractCardCode(raw.getOpponent()).orElse(null),
                raw);
    }

    private static BigDecimal winRate(long wins, long losses) {
        var total = wins + losses;
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(wins)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static <T> BigDecimal weightedAverage(List<T> items, Function<T, BigDecimal> valueFn,
                                                   Function<T, Long> weightFn) {
        var weightedSum = BigDecimal.ZERO;
        var totalWeight = 0L;
        for (var item : items) {
            var value = valueFn.apply(item);
            var weight = weightFn.apply(item);
            if (value == null || weight == null || weight <= 0) {
                continue;
            }
            weightedSum = weightedSum.add(value.multiply(BigDecimal.valueOf(weight)));
            totalWeight += weight;
        }
        if (totalWeight <= 0) {
            return null;
        }
        return weightedSum.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
    }

    private record NormalizedLeaderStatPair(String code, RawLeaderStat raw) {
    }

    private record NormalizedMatchupPair(String leaderCode, String opponentCode, RawMatchup raw) {
    }
}
