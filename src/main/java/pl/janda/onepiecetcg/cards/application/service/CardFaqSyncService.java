package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.client.CardFaqApiClient;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class CardFaqSyncService implements CardFaqSyncUseCase {

    private final CardFaqRepository cardFaqRepository;

    private final CardFaqApiClient cardFaqApiClient;

    private final CardFaqReplacementService cardFaqReplacementService;

    @Override
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
