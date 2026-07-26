package pl.janda.onepiecetcg.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.service.CardErrataSyncService;

@Component
@RequiredArgsConstructor
public class CardErrataSyncScheduler extends AbstractSyncScheduler {

    private final CardErrataSyncService cardErrataSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(cardErrataSyncService::syncErrata, "card errata");
    }

    @Scheduled(cron = "${card-errata.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(cardErrataSyncService::syncErrata, "card errata");
    }
}
