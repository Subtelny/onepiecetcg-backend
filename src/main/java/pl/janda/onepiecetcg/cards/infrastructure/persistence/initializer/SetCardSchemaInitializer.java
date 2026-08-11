package pl.janda.onepiecetcg.cards.infrastructure.persistence.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;


@Component
@RequiredArgsConstructor
@Slf4j
public class SetCardSchemaInitializer implements ApplicationRunner {

    private static final String DDL_SCRIPT = "db/set-cards-schema.sql";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        apply();
    }


    void apply() throws Exception {
        try (var connection = dataSource.getConnection()) {
            migrateLegacyCardIdColumn(connection);
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(DDL_SCRIPT));
        }
        log.info("Applied set_cards schema from {}", DDL_SCRIPT);
    }

    private void migrateLegacyCardIdColumn(java.sql.Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE set_cards ADD COLUMN IF NOT EXISTS card_id varchar(255)");
        }

        try (var legacyColumn = connection.getMetaData().getColumns(
                connection.getCatalog(), null, "set_cards", "card_image_id")) {
            if (!legacyColumn.next()) {
                return;
            }
        }

        try (var statement = connection.createStatement()) {
            var migratedRows = statement.executeUpdate(
                    "UPDATE set_cards SET card_id = card_image_id WHERE card_id IS NULL");
            statement.execute("ALTER TABLE set_cards DROP COLUMN card_image_id");
            log.info("Migrated {} set_cards rows from card_image_id to card_id", migratedRows);
        }
    }
}
