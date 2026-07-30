package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Applies the {@code card_semantic_search_vector} DDL (db/set-cards-search-vector.sql) on every
 * startup, so no environment can end up without it.
 * <p>
 * That column is a Postgres {@code GENERATED ALWAYS ... STORED} tsvector with a GIN index, which
 * Hibernate's ddl-auto cannot express, and it is deliberately not mapped on the SetCard entity
 * (see CLAUDE.md §3). It used to be applied by hand, once per environment - which silently left it
 * missing wherever that step was skipped, making every SEMANTIC search fail with an undefined-column
 * error that Spring surfaces as the misleading "bad SQL grammar" (Postgres reports it as SQLState
 * 42703, and Spring maps the whole 42 class to BadSqlGrammarException).
 * <p>
 * Runs as an ApplicationRunner rather than an ApplicationReadyEvent listener because runners execute
 * before that event is published, and therefore before the sync schedulers' startup jobs, which write
 * to set_cards and recompute over it. Hibernate has already created the table by this point.
 * <p>
 * Failure here fails startup on purpose: booting into a state where card search is guaranteed to
 * return 500s is worse than not booting at all.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class SetCardSearchVectorSchemaInitializer implements ApplicationRunner {

    private static final String DDL_SCRIPT = "db/set-cards-search-vector.sql";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        apply();
    }

    /**
     * Idempotent: the script is written with IF NOT EXISTS, so an already-migrated database is a
     * no-op and the stored tsvector is never rebuilt on a normal boot.
     */
    void apply() throws Exception {
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(DDL_SCRIPT));
        }
        log.info("Applied set_cards search vector schema from {}", DDL_SCRIPT);
    }
}
