package pl.janda.onepiecetcg.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.service.CardSetSyncService;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardSetSyncScheduler {

    private final CardSetSyncService cardSetSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely();
    }

    @Scheduled(cron = "${sets.sync.cron}")
    public void scheduledSync() {
        runSyncSafely();
    }

    private void runSyncSafely() {
        try {
            cardSetSyncService.syncSets();
        } catch (Exception e) {
            log.error("Failed to sync card sets from optcgapi.com", e);
        }
    }
}
