package pl.janda.onepiecetcg.pricing.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSyncScheduler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected void runSyncSafely(Runnable syncTask, String syncDescription) {
        try {
            syncTask.run();
        } catch (Exception e) {
            log.error("Failed to sync {}", syncDescription, e);
        }
    }
}
