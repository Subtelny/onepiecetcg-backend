package pl.janda.onepiecetcg.pricing.infrastructure.persistence.initializer;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class CardmarketPriceHistorySchemaInitializerTest {

    @Test
    void run_allowsTheGlobalUniqueCodeMatchType() throws Exception {
        var dataSource = mock(DataSource.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        new CardmarketPriceHistorySchemaInitializer(dataSource).run(null);

        verify(statement).execute(contains("DROP CONSTRAINT IF EXISTS cardmarket_single_mappings_match_type_check"));
        verify(statement).execute(contains("'CODE_SINGLE_MATCH'"));
    }
}
