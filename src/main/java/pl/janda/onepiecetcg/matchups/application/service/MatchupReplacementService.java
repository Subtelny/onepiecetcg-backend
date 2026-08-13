package pl.janda.onepiecetcg.matchups.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeader;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCard;
import pl.janda.onepiecetcg.matchups.application.model.MatchupPair;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderCardRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupLeaderRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupPairRepository;
import pl.janda.onepiecetcg.matchups.application.repository.MatchupSnapshotInfoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchupReplacementService {

    private final MatchupSnapshotInfoRepository snapshotInfoRepository;

    private final MatchupLeaderRepository leaderRepository;

    private final MatchupLeaderCardRepository leaderCardRepository;

    private final MatchupPairRepository pairRepository;

    @Transactional
    public void replaceAll(MatchupSnapshotInfo snapshotInfo, List<MatchupLeader> leaders, List<MatchupPair> pairs,
                           List<MatchupLeaderCard> leaderCards) {
        log.info("Deleting existing matchup data from database");
        var deleteStartTime = System.currentTimeMillis();
        leaderCardRepository.deleteAll();
        pairRepository.deleteAll();
        leaderRepository.deleteAll();
        snapshotInfoRepository.deleteAll();
        var deleteDuration = System.currentTimeMillis() - deleteStartTime;
        log.info("Deleted existing matchup data in {}ms", deleteDuration);

        log.info("Saving {} matchup leaders, {} matchup pairs and {} leader cards to database",
                leaders.size(), pairs.size(), leaderCards.size());
        var saveStartTime = System.currentTimeMillis();
        snapshotInfoRepository.save(snapshotInfo);
        leaderRepository.saveAll(leaders);
        pairRepository.saveAll(pairs);
        leaderCardRepository.saveAll(leaderCards);
        var saveDuration = System.currentTimeMillis() - saveStartTime;
        log.info("Successfully saved matchup data in {}ms", saveDuration);

        log.info("Matchup replacement completed - Breakdown: delete={}ms, save={}ms", deleteDuration, saveDuration);
    }
}
