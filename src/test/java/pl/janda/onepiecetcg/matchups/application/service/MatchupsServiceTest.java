package pl.janda.onepiecetcg.matchups.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCard;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderCardRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupPairRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupSnapshotInfoRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchupsServiceTest {

    @Mock
    private MatchupSnapshotInfoRepository snapshotInfoRepository;

    @Mock
    private MatchupLeaderRepository leaderRepository;

    @Mock
    private MatchupLeaderCardRepository leaderCardRepository;

    @Mock
    private MatchupPairRepository pairRepository;

    @Test
    void getMatchups_assemblesOverviewFromTheThreeOwnRepositories() {
        var snapshot = MatchupSnapshotInfo.builder().dataset("lw").totalMatches(131195L).build();
        var leader = MatchupLeader.builder().cardCode("OP14-020").name("Dracule Mihawk").build();
        var pair = MatchupPair.builder().leaderCode("OP14-020").opponentCode("OP13-079").build();
        when(snapshotInfoRepository.findCurrent()).thenReturn(Optional.of(snapshot));
        when(leaderRepository.findAllOrderByPopularityDesc()).thenReturn(List.of(leader));
        when(leaderCardRepository.findAllOrderByLeaderAndCategoryAndInclusionRate()).thenReturn(List.of());
        when(pairRepository.findAll()).thenReturn(List.of(pair));
        var service = new MatchupsService(snapshotInfoRepository, leaderRepository, leaderCardRepository, pairRepository);

        var overview = service.getMatchups();

        assertThat(overview.snapshot()).isEqualTo(snapshot);
        assertThat(overview.leaders()).containsExactly(leader);
        assertThat(overview.leaderCards()).isEmpty();
        assertThat(overview.matchups()).containsExactly(pair);
        // "OP13-079" isn't itself a known leader here, so the pair can't be a top-leaders pairing.
        assertThat(overview.topMatchups()).isEmpty();
    }

    @Test
    void getMatchups_toleratesMissingSnapshotByReturningNull() {
        when(snapshotInfoRepository.findCurrent()).thenReturn(Optional.empty());
        when(leaderRepository.findAllOrderByPopularityDesc()).thenReturn(List.of());
        when(leaderCardRepository.findAllOrderByLeaderAndCategoryAndInclusionRate()).thenReturn(List.of());
        when(pairRepository.findAll()).thenReturn(List.of());
        var service = new MatchupsService(snapshotInfoRepository, leaderRepository, leaderCardRepository, pairRepository);

        var overview = service.getMatchups();

        assertThat(overview.snapshot()).isNull();
        assertThat(overview.leaders()).isEmpty();
        assertThat(overview.leaderCards()).isEmpty();
        assertThat(overview.matchups()).isEmpty();
        assertThat(overview.topMatchups()).isEmpty();
    }

    @Test
    void getMatchups_topMatchupsOnlyIncludesPairsAmongTheTenMostPopularLeaders() {
        var leaders = IntStream.range(0, 12)
                .mapToObj(i -> MatchupLeader.builder()
                        .cardCode("L" + i)
                        .name("Leader " + i)
                        // Repository contract is "already sorted desc by popularity" - this list
                        // reflects that ordering directly rather than re-deriving it here.
                        .popularity(BigDecimal.valueOf(100 - i))
                        .build())
                .toList();
        var topPair = MatchupPair.builder().leaderCode("L0").opponentCode("L9").build();
        var pairInvolvingAnEleventhRankedLeader = MatchupPair.builder().leaderCode("L0").opponentCode("L10").build();
        when(snapshotInfoRepository.findCurrent()).thenReturn(Optional.empty());
        when(leaderRepository.findAllOrderByPopularityDesc()).thenReturn(leaders);
        when(leaderCardRepository.findAllOrderByLeaderAndCategoryAndInclusionRate()).thenReturn(List.of());
        when(pairRepository.findAll()).thenReturn(List.of(topPair, pairInvolvingAnEleventhRankedLeader));
        var service = new MatchupsService(snapshotInfoRepository, leaderRepository, leaderCardRepository, pairRepository);

        var overview = service.getMatchups();

        assertThat(overview.topMatchups()).containsExactly(topPair);
    }

    @Test
    void getOverview_fetchesOnlyTheTopLeaderMatrixAndSkipsCardProfiles() {
        var snapshot = MatchupSnapshotInfo.builder().dataset("lw").totalMatches(131195L).build();
        var leaders = IntStream.range(0, 12)
                .mapToObj(i -> MatchupLeader.builder().cardCode("L" + i).name("Leader " + i).build())
                .toList();
        var topLeaderCodes = Set.of("L0", "L1", "L2", "L3", "L4", "L5", "L6", "L7", "L8", "L9");
        var topPair = MatchupPair.builder().leaderCode("L0").opponentCode("L9").build();
        when(snapshotInfoRepository.findCurrent()).thenReturn(Optional.of(snapshot));
        when(leaderRepository.findAllOrderByPopularityDesc()).thenReturn(leaders);
        when(pairRepository.findByLeaderCodes(topLeaderCodes)).thenReturn(List.of(topPair));
        var service = new MatchupsService(snapshotInfoRepository, leaderRepository, leaderCardRepository, pairRepository);

        var overview = service.getOverview();

        assertThat(overview.snapshot()).isEqualTo(snapshot);
        assertThat(overview.leaders()).containsExactlyElementsOf(leaders);
        assertThat(overview.topMatchups()).containsExactly(topPair);
        verifyNoInteractions(leaderCardRepository);
    }

    @Test
    void getLeaderMatchups_normalizesCodeAndFetchesOnlyThatLeadersData() {
        var snapshot = MatchupSnapshotInfo.builder().dataset("lw").totalMatches(131195L).build();
        var leader = MatchupLeader.builder().cardCode("OP14-020").name("Dracule Mihawk").build();
        var card = MatchupLeaderCard.builder().leaderCode("OP14-020").cardCode("OP01-001").build();
        var pair = MatchupPair.builder().leaderCode("OP14-020").opponentCode("OP13-079").build();
        when(leaderRepository.findByCode("OP14-020")).thenReturn(Optional.of(leader));
        when(snapshotInfoRepository.findCurrent()).thenReturn(Optional.of(snapshot));
        when(leaderCardRepository.findByLeaderCode("OP14-020")).thenReturn(List.of(card));
        when(pairRepository.findByLeaderCode("OP14-020")).thenReturn(List.of(pair));
        var service = new MatchupsService(snapshotInfoRepository, leaderRepository, leaderCardRepository, pairRepository);

        var result = service.getLeaderMatchups(" op14-020 ");

        assertThat(result).hasValueSatisfying(detail -> {
            assertThat(detail.snapshot()).isEqualTo(snapshot);
            assertThat(detail.leader()).isEqualTo(leader);
            assertThat(detail.leaderCards()).containsExactly(card);
            assertThat(detail.matchups()).containsExactly(pair);
        });
    }

    @Test
    void getLeaderMatchups_unknownLeaderDoesNotQueryCardsOrPairs() {
        when(leaderRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());
        var service = new MatchupsService(snapshotInfoRepository, leaderRepository, leaderCardRepository, pairRepository);

        var result = service.getLeaderMatchups("unknown");

        assertThat(result).isEmpty();
        verifyNoInteractions(snapshotInfoRepository, leaderCardRepository, pairRepository);
    }
}
