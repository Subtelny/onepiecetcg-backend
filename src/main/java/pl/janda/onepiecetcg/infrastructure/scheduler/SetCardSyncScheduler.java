package pl.janda.onepiecetcg.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.service.SetCardSyncService;

@Component
@RequiredArgsConstructor
public class SetCardSyncScheduler extends AbstractSyncScheduler {

    private final SetCardSyncService setCardSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(setCardSyncService::syncSetCards, "set cards");
    }

    @Scheduled(cron = "${set-cards.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(setCardSyncService::syncSetCards, "set cards");
    }
}
