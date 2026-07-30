package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;

import java.util.List;

/**
 * Owns the transactional write half of the FAQ sync: replace one set's entries in one transaction.
 * <p>
 * Split out of CardFaqSyncService so the per-set PDF download and parse happen outside any transaction
 * (CLAUDE.md §7). Scoping the transaction to a single set is deliberate on two counts: the delete is
 * already set-scoped, so per-set atomicity is the correct granularity, and it bounds the persistence
 * context to one set's rows instead of accumulating every set's for the whole run.
 */
@Service
@RequiredArgsConstructor
public class CardFaqReplacementService {

    private final CardFaqRepository cardFaqRepository;

    @Transactional
    public void replaceSet(String setId, List<CardFaq> faqEntries) {
        cardFaqRepository.deleteBySetId(setId);
        cardFaqRepository.saveAll(faqEntries);
    }
}
