package pl.janda.onepiecetcg.cards.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;

@Component
@RequiredArgsConstructor
public class SetCardSyncScheduler extends AbstractSyncScheduler {

    private final SetCardSyncUseCase setCardSyncUseCase;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(setCardSyncUseCase::syncSetCards, "set cards");
    }

    @Scheduled(cron = "${set-cards.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(setCardSyncUseCase::syncSetCards, "set cards");
    }
}
