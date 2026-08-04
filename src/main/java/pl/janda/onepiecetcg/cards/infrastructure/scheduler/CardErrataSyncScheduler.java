package pl.janda.onepiecetcg.cards.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;

@Component
@RequiredArgsConstructor
public class CardErrataSyncScheduler extends AbstractSyncScheduler {

    private final CardErrataSyncUseCase cardErrataSyncUseCase;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(cardErrataSyncUseCase::syncErrata, "card errata");
    }

    @Scheduled(cron = "${card-errata.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(cardErrataSyncUseCase::syncErrata, "card errata");
    }
}
