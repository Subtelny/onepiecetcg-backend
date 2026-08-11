package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.repository.SetCardCommandRepository;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class SetCardReplacementService {

    private final SetCardCommandRepository setCardRepository;

    private final CardFilterOptionService cardFilterOptionService;

    @Transactional
    public void replaceAll(List<SetCard> setCards) {
        log.info("Deleting existing set cards from database");
        var deleteStartTime = System.currentTimeMillis();
        setCardRepository.deleteAll();
        var deleteDuration = System.currentTimeMillis() - deleteStartTime;
        log.info("Deleted existing set cards in {}ms", deleteDuration);

        log.info("Saving {} set cards to database (this may take several minutes for large datasets)", setCards.size());
        var saveStartTime = System.currentTimeMillis();
        setCardRepository.saveAll(setCards);
        var saveDuration = System.currentTimeMillis() - saveStartTime;
        log.info("Successfully saved {} set cards to database in {}ms ({} seconds)",
                setCards.size(), saveDuration, saveDuration / 1000);

        log.info("Refreshing card filter options cache");
        var refreshStartTime = System.currentTimeMillis();
        cardFilterOptionService.refresh();
        var refreshDuration = System.currentTimeMillis() - refreshStartTime;
        log.info("Card filter options cache refreshed successfully in {}ms", refreshDuration);

        log.info("Set cards replacement completed - Breakdown: delete={}ms, save={}ms, refresh={}ms",
                deleteDuration, saveDuration, refreshDuration);
    }
}
