package pl.janda.onepiecetcg.matchups.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.matchups.application.model.*;
import pl.janda.onepiecetcg.matchups.application.repository.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchupSyncServiceTest {

    @Mock
    private RawMatchupSnapshotRepository rawSnapshotRepository;

    @Mock
    private RawLeaderStatRepository rawLeaderStatRepository;

    @Mock
    private RawMatchupRepository rawMatchupRepository;

    @Mock
    private RawDecklistRepository rawDecklistRepository;

    @Mock
    private MatchupNormalizationService normalizationService;

    @Mock
    private MatchupCardProfileService cardProfileService;

    @Mock
    private CardCatalogUseCase cardCatalogUseCase;

    @Mock
    private MatchupReplacementService matchupReplacementService;

    @Mock
    private MatchupSnapshotInfoRepository matchupSnapshotInfoRepository;

    @Mock
    private MatchupLeaderRepository leaderRepository;

    private MatchupSyncService service() {
        return new MatchupSyncService(rawSnapshotRepository, rawLeaderStatRepository, rawMatchupRepository,
                rawDecklistRepository, normalizationService, cardProfileService, cardCatalogUseCase,
                matchupReplacementService, matchupSnapshotInfoRepository, leaderRepository);
    }

    @Test
    void syncMatchups_returnsFalseAndSkipsReplacementWhenNoSnapshotExists() {
        when(rawSnapshotRepository.findLatest()).thenReturn(Optional.empty());

        var result = service().syncMatchups();

        assertThat(result).isFalse();
        verify(matchupReplacementService, never()).replaceAll(any(), any(), any(), any());
    }

    @Test
    void syncMatchups_returnsFalseAndSkipsReplacementWhenSnapshotAlreadySynced() {
        var scrapedAt = OffsetDateTime.now();
        var snapshot = RawMatchupSnapshot.builder()
                .id(1L).dataset("lw").totalMatches(100L).scrapedAt(scrapedAt)
                .build();
        when(rawSnapshotRepository.findLatest()).thenReturn(Optional.of(snapshot));
        when(matchupSnapshotInfoRepository.findCurrent()).thenReturn(Optional.of(
                MatchupSnapshotInfo.builder().sourceSnapshotId(1L).dataset("lw")
                        .totalMatches(100L).scrapedAt(scrapedAt).build()));
        when(leaderRepository.hasAnyRepresentativeDeck()).thenReturn(true);

        var result = service().syncMatchups();

        assertThat(result).isFalse();
        verify(rawLeaderStatRepository, never()).findBySnapshotId(any());
        verify(matchupReplacementService, never()).replaceAll(any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void syncMatchups_excludesResolvedCardsThatAreNotLeaderTypeFromReplacement() {
        var snapshot = RawMatchupSnapshot.builder()
                .id(1L).dataset("lw").totalMatches(100L).scrapedAt(OffsetDateTime.now())
                .build();
        when(rawSnapshotRepository.findLatest()).thenReturn(Optional.of(snapshot));
        when(rawLeaderStatRepository.findBySnapshotId(1L)).thenReturn(List.of(
                RawLeaderStat.builder().leader("1xOP14-020").build(),
                RawLeaderStat.builder().leader("4xST34-003").build()));
        when(rawMatchupRepository.findBySnapshotId(1L)).thenReturn(List.of(RawMatchup.builder().build()));

        var leaderStat = new NormalizedLeaderStat("OP14-020", 100L, BigDecimal.valueOf(50), BigDecimal.valueOf(20));
        var characterStat = new NormalizedLeaderStat("ST34-003", 3L, BigDecimal.ZERO, BigDecimal.ZERO);
        when(normalizationService.normalizeAndMergeLeaderStats(anyList()))
                .thenReturn(List.of(leaderStat, characterStat));

        var leaderCard = SetCard.builder().cardSetId("OP14-020").cardName("Dracule Mihawk")
                .cardColor("GREEN").cardImage("https://cdn.example/op14-020.png").cardType("Leader").build();
        var characterCard = SetCard.builder().cardSetId("ST34-003").cardName("Imu")
                .cardType("Character").build();
        when(cardCatalogUseCase.getRepresentativeCardsByCardCodes(anyList()))
                .thenReturn(List.of(leaderCard, characterCard));

        when(normalizationService.normalizeAndMergeMatchups(anyList(), any())).thenReturn(List.of());
        when(rawDecklistRepository.findBySnapshotId(1L)).thenReturn(List.of());
        when(cardProfileService.calculateProfiles(anyList(), any())).thenReturn(List.of());

        service().syncMatchups();

        var validLeaderCodesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(normalizationService).normalizeAndMergeMatchups(anyList(), validLeaderCodesCaptor.capture());
        assertThat(validLeaderCodesCaptor.getValue()).containsExactly("OP14-020");

        var snapshotCaptor = ArgumentCaptor.forClass(MatchupSnapshotInfo.class);
        var leadersCaptor = ArgumentCaptor.forClass(List.class);
        var pairsCaptor = ArgumentCaptor.forClass(List.class);
        verify(matchupReplacementService).replaceAll(snapshotCaptor.capture(), leadersCaptor.capture(),
                pairsCaptor.capture(), anyList());

        assertThat(snapshotCaptor.getValue().getSourceSnapshotId()).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        var savedLeaders = (List<MatchupLeader>) leadersCaptor.getValue();
        assertThat(savedLeaders).extracting(MatchupLeader::getCardCode).containsExactly("OP14-020");
        assertThat(savedLeaders).extracting(MatchupLeader::getName).containsExactly("Dracule Mihawk");

        @SuppressWarnings("unchecked")
        var savedPairs = (List<MatchupPair>) pairsCaptor.getValue();
        assertThat(savedPairs).isEmpty();
    }
}
