package pl.janda.onepiecetcg.matchups.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.infrastructure.scheduler.AbstractSyncScheduler;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;

@Component
@RequiredArgsConstructor
public class MatchupSyncScheduler extends AbstractSyncScheduler {

    private final MatchupSyncUseCase matchupSyncUseCase;

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSyncSafely(matchupSyncUseCase::syncMatchups, "matchups");
    }

    @Scheduled(cron = "${matchups.sync.cron}")
    public void scheduledSync() {
        runSyncSafely(matchupSyncUseCase::syncMatchups, "matchups");
    }
}
