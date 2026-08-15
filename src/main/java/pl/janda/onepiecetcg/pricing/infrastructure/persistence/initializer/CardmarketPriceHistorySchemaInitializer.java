package pl.janda.onepiecetcg.pricing.infrastructure.persistence.initializer;

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

    private static final String DROP_SINGLE_MATCH_TYPE_CONSTRAINT = """
            ALTER TABLE IF EXISTS cardmarket_single_mappings
            DROP CONSTRAINT IF EXISTS cardmarket_single_mappings_match_type_check
            """;

    private static final String ADD_SINGLE_MATCH_TYPE_CONSTRAINT = """
            ALTER TABLE IF EXISTS cardmarket_single_mappings
            ADD CONSTRAINT cardmarket_single_mappings_match_type_check
            CHECK (match_type IN (
                'CODE_SINGLE_MATCH',
                'CODE_EXPANSION_SINGLE_MATCH',
                'CODE_EXPANSION_VERSION',
                'CODE_EXPANSION_ORDER_HEURISTIC'
            ))
            """;

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(DROP_OBSOLETE_CONSTRAINT);
            statement.execute(DROP_SINGLE_MATCH_TYPE_CONSTRAINT);
            statement.execute(ADD_SINGLE_MATCH_TYPE_CONSTRAINT);
        }
        log.info("Cardmarket price history schema is ready");
    }
}
