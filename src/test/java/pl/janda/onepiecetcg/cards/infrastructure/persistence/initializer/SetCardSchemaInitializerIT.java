package pl.janda.onepiecetcg.cards.infrastructure.persistence.initializer;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pl.janda.onepiecetcg.OnePieceTcgApplication;
import pl.janda.onepiecetcg.cards.application.port.in.*;
import pl.janda.onepiecetcg.cards.application.repository.SetCardCommandRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private SetCardCommandRepository setCardCommandRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

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
    void apply_createsVariantDisplayColumns() throws Exception {
        dsl.execute("ALTER TABLE set_cards DROP COLUMN IF EXISTS display_name");
        dsl.execute("ALTER TABLE set_cards DROP COLUMN IF EXISTS source_product");

        initializer.apply();

        var columns = dsl.fetch("""
                select column_name from information_schema.columns
                 where table_name = 'set_cards'
                   and column_name in ('display_name', 'source_product')
                order by column_name
                """).getValues("column_name", String.class);

        assertThat(columns).containsExactly("display_name", "source_product");
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

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting in concurrency test", exception);
        }
    }

    @Test
    void apply_migratesIntegerVariantIndexAndBackfillsSourceDerivedCodes() throws Exception {
        dsl.execute("DELETE FROM set_cards");
        dsl.execute("ALTER TABLE set_cards DROP COLUMN variant_index");
        dsl.execute("ALTER TABLE set_cards ADD COLUMN variant_index integer NOT NULL DEFAULT 0");
        dsl.execute("""
                INSERT INTO set_cards (card_id, variant_index)
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
                SELECT variant_index FROM set_cards ORDER BY card_id
                """).getValues("variant_index", String.class);
        assertThat(dataType).isEqualTo("character varying");
        assertThat(indexes).containsExactly("0", "p1", "r1");
    }

    @Test
    void apply_migratesLegacyCardImageId_deduplicatesAndCreatesUniqueIndex() throws Exception {
        dsl.execute("DELETE FROM set_cards");
        dsl.execute("DROP INDEX IF EXISTS uk_set_cards_card_id");
        dsl.execute("ALTER TABLE set_cards ADD COLUMN IF NOT EXISTS card_image_id varchar(255)");
        dsl.execute("""
                INSERT INTO set_cards (card_image_id, display_name, last_synced_at)
                VALUES ('P-102_p2', 'older', timestamp '2026-08-11 03:59:31'),
                       ('P-102_p2', 'newer', timestamp '2026-08-11 03:59:37')
                """);

        initializer.apply();

        var migrated = dsl.fetch("SELECT card_id, display_name FROM set_cards");
        var oldColumnCount = dsl.fetchValue("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_name = 'set_cards' AND column_name = 'card_image_id'
                """);
        var indexDefinition = dsl.fetchValue("""
                SELECT indexdef FROM pg_indexes
                WHERE tablename = 'set_cards' AND indexname = 'uk_set_cards_card_id'
                """);

        assertThat(migrated).hasSize(1);
        assertThat(migrated.getFirst().get("card_id", String.class)).isEqualTo("P-102_p2");
        assertThat(migrated.getFirst().get("display_name", String.class)).isEqualTo("newer");
        assertThat(oldColumnCount).hasToString("0");
        assertThat(indexDefinition.toString()).contains("UNIQUE", "card_id");
    }

    @Test
    void replacementLock_serializesConcurrentTransactions() throws Exception {
        var firstAcquired = new CountDownLatch(1);
        var releaseFirst = new CountDownLatch(1);
        var secondStarted = new CountDownLatch(1);
        var secondAcquired = new CountDownLatch(1);
        var transactions = new TransactionTemplate(transactionManager);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> transactions.executeWithoutResult(status -> {
                setCardCommandRepository.lockForReplacement();
                firstAcquired.countDown();
                await(releaseFirst);
            }));
            assertThat(firstAcquired.await(5, TimeUnit.SECONDS)).isTrue();

            var second = executor.submit(() -> transactions.executeWithoutResult(status -> {
                secondStarted.countDown();
                setCardCommandRepository.lockForReplacement();
                secondAcquired.countDown();
            }));
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondAcquired.await(250, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            assertThat(secondAcquired.getCount()).isZero();
        } finally {
            releaseFirst.countDown();
        }
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
