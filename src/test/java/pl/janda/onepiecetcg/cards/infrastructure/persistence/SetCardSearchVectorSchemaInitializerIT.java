package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.service.CardErrataSyncService;
import pl.janda.onepiecetcg.cards.application.service.CardFaqSyncService;
import pl.janda.onepiecetcg.cards.application.service.CardmarketPriceSyncService;
import pl.janda.onepiecetcg.cards.application.service.CardSetSyncService;
import pl.janda.onepiecetcg.cards.application.service.SetCardSyncService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Guards the schema half of SEMANTIC search. The generated card_semantic_search_vector column used to
 * be applied by hand, once per environment; wherever that was missed, every SEMANTIC search failed at
 * runtime with an undefined-column error surfaced as BadSqlGrammarException. These tests assert the
 * column and its GIN index exist purely as a result of the application starting, against a database
 * that begins with neither.
 */
@SpringBootTest(classes = OnePieceTcgApplication.class)
@Testcontainers
class SetCardSearchVectorSchemaInitializerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&prepareThreshold=0");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean
    private SetCardSyncService setCardSyncService;

    @MockitoBean
    private CardSetSyncService cardSetSyncService;

    @MockitoBean
    private CardErrataSyncService cardErrataSyncService;

    @MockitoBean
    private CardFaqSyncService cardFaqSyncService;

    @MockitoBean
    private CardmarketPriceSyncService cardmarketPriceSyncService;

    @Autowired
    private SetCardSearchVectorSchemaInitializer initializer;

    @Autowired
    private DSLContext dsl;

    @Test
    void startup_createsGeneratedSearchVectorColumn() {
        var generationExpression = dsl.fetchValue("""
                select generation_expression from information_schema.columns
                 where table_name = 'set_cards' and column_name = 'card_semantic_search_vector'
                """);

        // Present at all, and actually GENERATED - a plain nullable column would always be empty and
        // silently match nothing rather than failing loudly.
        assertThat(generationExpression).isNotNull();
        assertThat(generationExpression.toString()).contains("setweight");
    }

    @Test
    void startup_createsGinIndexBackingTheSearchVector() {
        var indexDefinition = dsl.fetchValue("""
                select indexdef from pg_indexes
                 where tablename = 'set_cards' and indexname = 'idx_set_cards_card_semantic_search_vector'
                """);

        assertThat(indexDefinition).isNotNull();
        assertThat(indexDefinition.toString()).contains("gin");
    }

    @Test
    void apply_isIdempotent_soEveryRestartIsANoOp() {
        // The script runs on every boot, not just the first, so re-applying it against an
        // already-migrated database must neither fail nor rebuild the stored tsvector.
        assertThatCode(() -> {
            initializer.apply();
            initializer.apply();
        }).doesNotThrowAnyException();

        var columnCount = dsl.fetchValue("""
                select count(*) from information_schema.columns
                 where table_name = 'set_cards' and column_name = 'card_semantic_search_vector'
                """);
        assertThat(columnCount).hasToString("1");
    }
}
