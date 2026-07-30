package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.client.CardFaqApiClient;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;

import java.time.LocalDateTime;

/**
 * Orchestrates the FAQ sync: list the published PDFs, skip the sets whose stored published date already
 * matches, then download, parse and hand each remaining set to CardFaqReplacementService.
 * <p>
 * Deliberately not @Transactional. Every set means an HTTP download of a whole PDF plus a PDFBox parse,
 * and the previous single transaction around the entire loop held a connection for all of them at once
 * (CLAUDE.md §7). The up-to-date check below is a plain read, so it does not need one either.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardFaqSyncService {

    private final CardFaqRepository cardFaqRepository;

    private final CardFaqApiClient cardFaqApiClient;

    private final CardFaqReplacementService cardFaqReplacementService;

    public void syncFaq() {
        var listing = cardFaqApiClient.fetchFaqListing();

        for (var entry : listing) {
            var alreadyUpToDate = cardFaqRepository.findBySetId(entry.setId()).stream()
                    .anyMatch(faq -> entry.publishedDate().equals(faq.getPublishedDate()));
            if (alreadyUpToDate) {
                continue;
            }

            var parsed = cardFaqApiClient.fetchFaqEntries(entry.setId(), entry.publishedDate(), entry.pdfUrl());
            var now = LocalDateTime.now();
            parsed.forEach(faq -> faq.setLastSyncedAt(now));

            cardFaqReplacementService.replaceSet(entry.setId(), parsed);
            log.info("Synced {} FAQ entries for set {} (published {})", parsed.size(), entry.setId(), entry.publishedDate());
        }
    }
}
