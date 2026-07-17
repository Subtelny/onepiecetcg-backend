package pl.janda.onepiecetcg.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.service.CardSetSyncService;

@Component
@RequiredArgsConstructor
public class CardSetSyncScheduler extends AbstractSyncScheduler {

    private final CardSetSyncService cardSetSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(cardSetSyncService::syncCardSets, "card sets");
    }

    @Scheduled(cron = "${card-sets.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(cardSetSyncService::syncCardSets, "card sets");
    }
}
