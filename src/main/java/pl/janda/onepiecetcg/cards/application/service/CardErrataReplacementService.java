package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardErrata;
import pl.janda.onepiecetcg.cards.application.repository.CardErrataRepository;

import java.util.List;

/**
 * Owns the transactional write half of the errata sync: replace the whole table in one transaction.
 * <p>
 * Split out of CardErrataSyncService so the transaction starts after the scrape has finished. The
 * scrape is a page fetch per errata notice, and holding a connection plus the delete's row locks for
 * its full duration is exactly what CLAUDE.md §7 forbids.
 * <p>
 * This is a transaction boundary, not an abstraction for reuse - delete-all followed by the insert must
 * be atomic, or a failure part-way through leaves the errata table empty.
 */
@Service
@RequiredArgsConstructor
public class CardErrataReplacementService {

    private final CardErrataRepository cardErrataRepository;

    @Transactional
    public void replaceAll(List<CardErrata> errata) {
        cardErrataRepository.deleteAll();
        cardErrataRepository.saveAll(errata);
    }
}
