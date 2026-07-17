package pl.janda.onepiecetcg.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.service.PromoCardSyncService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromoCardSyncScheduler {

    private final PromoCardSyncService promoCardSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely();
    }

    @Scheduled(cron = "${promo-cards.sync.cron}")
    public void scheduledSync() {
        runSyncSafely();
    }

    private void runSyncSafely() {
        try {
            promoCardSyncService.syncPromoCards();
        } catch (Exception e) {
            log.error("Failed to sync promo cards from optcgapi.com", e);
        }
    }
}
