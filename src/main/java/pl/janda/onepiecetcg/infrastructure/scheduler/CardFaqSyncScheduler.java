package pl.janda.onepiecetcg.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.service.CardFaqSyncService;

@Component
@RequiredArgsConstructor
public class CardFaqSyncScheduler extends AbstractSyncScheduler {

    private final CardFaqSyncService cardFaqSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(cardFaqSyncService::syncFaq, "card faq");
    }

    @Scheduled(cron = "${card-faq.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(cardFaqSyncService::syncFaq, "card faq");
    }
}
