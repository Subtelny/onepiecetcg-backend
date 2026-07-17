package pl.janda.onepiecetcg.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.service.SetCardSyncService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SetCardSyncScheduler {

    private final SetCardSyncService setCardSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely();
    }

    @Scheduled(cron = "${set-cards.sync.cron}")
    public void scheduledSync() {
        runSyncSafely();
    }

    private void runSyncSafely() {
        try {
            setCardSyncService.syncSetCards();
        } catch (Exception e) {
            log.error("Failed to sync set cards from optcgapi.com", e);
        }
    }
}
