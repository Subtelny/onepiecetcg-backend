package pl.janda.onepiecetcg.cards.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;

@Component
@RequiredArgsConstructor
public class CardFaqSyncScheduler extends AbstractSyncScheduler {

    private final CardFaqSyncUseCase cardFaqSyncUseCase;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(cardFaqSyncUseCase::syncFaq, "card faq");
    }

    @Scheduled(cron = "${card-faq.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(cardFaqSyncUseCase::syncFaq, "card faq");
    }
}
