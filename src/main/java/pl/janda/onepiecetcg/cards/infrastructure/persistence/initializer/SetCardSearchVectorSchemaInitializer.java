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
public class SetCardSearchVectorSchemaInitializer implements ApplicationRunner {

    private static final String DDL_SCRIPT = "db/set-cards-search-vector.sql";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        apply();
    }


    void apply() throws Exception {
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(DDL_SCRIPT));
        }
        log.info("Applied set_cards search vector schema from {}", DDL_SCRIPT);
    }
}
