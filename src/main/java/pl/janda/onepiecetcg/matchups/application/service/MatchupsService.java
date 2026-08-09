package pl.janda.onepiecetcg.matchups.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.model.MatchupsOverview;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupsQueryUseCase;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupPairRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupSnapshotInfoRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchupsService implements MatchupsQueryUseCase {

    private static final int TOP_LEADERS_COUNT = 10;

    private final MatchupSnapshotInfoRepository snapshotInfoRepository;

    private final MatchupLeaderRepository leaderRepository;

    private final MatchupPairRepository pairRepository;

    @Override
    public MatchupsOverview getMatchups() {
        var snapshot = snapshotInfoRepository.findCurrent().orElse(null);
        var leaders = leaderRepository.findAllOrderByPopularityDesc();
        var pairs = pairRepository.findAll();
        var topMatchups = filterToTopLeaders(leaders, pairs);
        return new MatchupsOverview(snapshot, leaders, pairs, topMatchups);
    }

    // The matrix on the matchups page only ever shows the most popular leaders, so
    // trimming this server-side keeps that grid's data small without limiting the
    // leader picker / head-to-head lookups, which still get the full `leaders`/`matchups`.
    private List<MatchupPair> filterToTopLeaders(List<MatchupLeader> leaders, List<MatchupPair> pairs) {
        Set<String> topLeaderCodes = leaders.stream()
                .limit(TOP_LEADERS_COUNT)
                .map(MatchupLeader::getCardCode)
                .collect(Collectors.toSet());
        return pairs.stream()
                .filter(pair -> topLeaderCodes.contains(pair.getLeaderCode()) && topLeaderCodes.contains(pair.getOpponentCode()))
                .toList();
    }
}
