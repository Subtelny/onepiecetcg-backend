package pl.janda.onepiecetcg.cards.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.port.in.CardmarketPriceSyncUseCase;

@Component
@RequiredArgsConstructor
public class CardmarketPriceSyncScheduler extends AbstractSyncScheduler {

    private final CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(cardmarketPriceSyncUseCase::syncPrices, "Cardmarket prices");
    }

    @Scheduled(cron = "${cardmarket.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(cardmarketPriceSyncUseCase::syncPrices, "Cardmarket prices");
    }
}
