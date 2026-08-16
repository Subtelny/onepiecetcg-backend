package pl.janda.onepiecetcg.testsupport;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class PostgresSpringBootTest {

    /**
     * Started once for the whole JVM instead of per test class: Spring caches an application context
     * across every test class sharing its configuration, so stopping the container after the first
     * class would leave the reused context pointing at a dead port. Ryuk stops it when the JVM exits.
     */
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "&prepareThreshold=0");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
