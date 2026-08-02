package pl.janda.onepiecetcg.cards.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Preserves compatibility with the first non-historical Cardmarket schema. That schema made
 * product_id globally unique, which prevents a product from having more than one daily snapshot.
 * Hibernate creates the current composite uniqueness constraint; this initializer only removes the
 * obsolete constraint and never updates or deletes imported rows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class CardmarketPriceHistorySchemaInitializer implements ApplicationRunner {

    private static final String DROP_OBSOLETE_CONSTRAINT = """
            ALTER TABLE IF EXISTS cardmarket_price_candidates
            DROP CONSTRAINT IF EXISTS uk_cardmarket_price_candidates_product_id
            """;

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(DROP_OBSOLETE_CONSTRAINT);
        }
        log.info("Cardmarket price history schema is ready");
    }
}
