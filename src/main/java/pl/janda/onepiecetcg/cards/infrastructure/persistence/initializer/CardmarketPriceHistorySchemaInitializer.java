package pl.janda.onepiecetcg.cards.infrastructure.persistence.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;


@Component
@RequiredArgsConstructor
@Slf4j
public class CardmarketPriceHistorySchemaInitializer implements ApplicationRunner {

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
