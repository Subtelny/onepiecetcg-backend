package pl.janda.onepiecetcg.matchups.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.matchups.application.model.*;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupsQueryUseCase;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderCardRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupPairRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupSnapshotInfoRepository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchupsService implements MatchupsQueryUseCase {

    private static final int TOP_LEADERS_COUNT = 10;

    private final MatchupSnapshotInfoRepository snapshotInfoRepository;

    private final MatchupLeaderRepository leaderRepository;

    private final MatchupLeaderCardRepository leaderCardRepository;

    private final MatchupPairRepository pairRepository;

    @Override
    public MatchupsOverview getMatchups() {
        var snapshot = snapshotInfoRepository.findCurrent().orElse(null);
        var leaders = leaderRepository.findAllOrderByPopularityDesc();
        var leaderCards = leaderCardRepository.findAllOrderByLeaderAndCategoryAndInclusionRate();
        var pairs = pairRepository.findAll();
        var topMatchups = filterToTopLeaders(leaders, pairs);
        return new MatchupsOverview(snapshot, leaders, leaderCards, pairs, topMatchups);
    }

    @Override
    public MatchupsSummary getOverview() {
        var snapshot = snapshotInfoRepository.findCurrent().orElse(null);
        var leaders = leaderRepository.findAllOrderByPopularityDesc();
        var topLeaderCodes = topLeaderCodes(leaders);
        var topMatchups = pairRepository.findByLeaderCodes(topLeaderCodes);
        return new MatchupsSummary(snapshot, leaders, topMatchups);
    }

    @Override
    public Optional<LeaderMatchups> getLeaderMatchups(String leaderCode) {
        var normalizedCode = leaderCode.trim().toUpperCase(Locale.ROOT);
        return leaderRepository.findByCode(normalizedCode)
                .map(leader -> new LeaderMatchups(
                        snapshotInfoRepository.findCurrent().orElse(null),
                        leader,
                        leaderCardRepository.findByLeaderCode(normalizedCode),
                        pairRepository.findByLeaderCode(normalizedCode)
                ));
    }

    // The matrix on the matchups page only ever shows the most popular leaders, so
    // trimming this server-side keeps that grid's data small without limiting the
    // leader picker / head-to-head lookups, which still get the full `leaders`/`matchups`.
    private List<MatchupPair> filterToTopLeaders(List<MatchupLeader> leaders, List<MatchupPair> pairs) {
        Set<String> topLeaderCodes = topLeaderCodes(leaders);
        return pairs.stream()
                .filter(pair -> topLeaderCodes.contains(pair.getLeaderCode()) && topLeaderCodes.contains(pair.getOpponentCode()))
                .toList();
    }

    private Set<String> topLeaderCodes(List<MatchupLeader> leaders) {
        return leaders.stream()
                .limit(TOP_LEADERS_COUNT)
                .map(MatchupLeader::getCardCode)
                .collect(Collectors.toSet());
    }
}
