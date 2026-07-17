package pl.janda.onepiecetcg.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.service.PromoCardSyncService;

@Component
@RequiredArgsConstructor
public class PromoCardSyncScheduler extends AbstractSyncScheduler {

    private final PromoCardSyncService promoCardSyncService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(promoCardSyncService::syncPromoCards, "promo cards");
    }

    @Scheduled(cron = "${promo-cards.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(promoCardSyncService::syncPromoCards, "promo cards");
    }
}
