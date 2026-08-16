package pl.janda.onepiecetcg.pricing.infrastructure.persistence.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Realigns the enum check constraints Hibernate writes once at table-creation time and never revisits
 * under {@code ddl-auto: update}. A database created before a constant was renamed keeps rejecting the
 * new value, so the sync fails at write time on migrated databases while passing on fresh ones.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PricingSchemaInitializer implements ApplicationRunner {

    private static final String DDL_SCRIPT = "db/pricing-schema.sql";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        apply();
    }

    public void apply() throws Exception {
        try (var connection = dataSource.getConnection()) {
            var originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource(DDL_SCRIPT));
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
        log.info("Applied pricing schema from {}", DDL_SCRIPT);
    }
}
