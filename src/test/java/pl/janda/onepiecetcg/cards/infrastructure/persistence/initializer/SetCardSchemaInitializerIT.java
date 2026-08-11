package pl.janda.onepiecetcg.cards.infrastructure.persistence.initializer;

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
import pl.janda.onepiecetcg.cards.application.port.in.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


@SpringBootTest(classes = OnePieceTcgApplication.class)
@Testcontainers
class SetCardSchemaInitializerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @MockitoBean
    private SetCardSyncUseCase setCardSyncUseCase;
    @MockitoBean
    private CardSetSyncUseCase cardSetSyncUseCase;
    @MockitoBean
    private CardErrataSyncUseCase cardErrataSyncUseCase;
    @MockitoBean
    private CardFaqSyncUseCase cardFaqSyncUseCase;
    @MockitoBean
    private CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;
    @Autowired
    private SetCardSchemaInitializer initializer;
    @Autowired
    private DSLContext dsl;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&prepareThreshold=0");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void startup_createsGeneratedSearchVectorColumn() {
        var generationExpression = dsl.fetchValue("""
                select generation_expression from information_schema.columns
                 where table_name = 'set_cards' and column_name = 'card_semantic_search_vector'
                """);


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
    void apply_removesObsoleteSetCardColumns() throws Exception {
        dsl.execute("ALTER TABLE set_cards ADD COLUMN IF NOT EXISTS is_representative boolean NOT NULL DEFAULT false");
        dsl.execute("ALTER TABLE set_cards ADD COLUMN IF NOT EXISTS date_scraped varchar(255)");
        dsl.execute("ALTER TABLE set_cards ADD COLUMN IF NOT EXISTS is_promo boolean NOT NULL DEFAULT false");

        initializer.apply();

        var columnCount = dsl.fetchValue("""
                select count(*) from information_schema.columns
                 where table_name = 'set_cards'
                   and column_name in ('is_representative', 'date_scraped', 'is_promo')
                """);
        assertThat(columnCount).hasToString("0");
    }

    @Test
    void apply_migratesIntegerVariantIndexAndBackfillsSourceDerivedCodes() throws Exception {
        dsl.execute("DELETE FROM set_cards");
        dsl.execute("ALTER TABLE set_cards DROP COLUMN variant_index");
        dsl.execute("ALTER TABLE set_cards ADD COLUMN variant_index integer NOT NULL DEFAULT 0");
        dsl.execute("""
                INSERT INTO set_cards (card_image_id, variant_index)
                VALUES ('OP16-079', 0),
                       ('OP16-079_p1', 1),
                       ('OP16-079_r1', 2)
                """);

        initializer.apply();

        var dataType = dsl.fetchValue("""
                SELECT data_type FROM information_schema.columns
                WHERE table_name = 'set_cards' AND column_name = 'variant_index'
                """);
        var indexes = dsl.fetch("""
                SELECT variant_index FROM set_cards ORDER BY card_image_id
                """).getValues("variant_index", String.class);
        assertThat(dataType).isEqualTo("character varying");
        assertThat(indexes).containsExactly("0", "p1", "r1");
    }

    @Test
    void apply_isIdempotent_soEveryRestartIsANoOp() {


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
