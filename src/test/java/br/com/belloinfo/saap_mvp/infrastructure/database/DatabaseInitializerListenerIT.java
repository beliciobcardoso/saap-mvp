package br.com.belloinfo.saap_mvp.infrastructure.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Teste de integração que exercita o caminho real de conexão JDBC do
 * {@link DatabaseInitializerListener}, usando um Postgres real via
 * Testcontainers (mesma imagem usada em BaseIntegrationTest) e chamando o
 * listener diretamente, sem subir o contexto Spring.
 */
@Testcontainers
@ExtendWith(MockitoExtension.class)
class DatabaseInitializerListenerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Mock
    private ApplicationEnvironmentPreparedEvent event;

    @Mock
    private ConfigurableEnvironment environment;

    @BeforeAll
    static void startContainer() {
        postgres.start();
    }

    @AfterAll
    static void stopContainer() {
        postgres.stop();
    }

    @Test
    @DisplayName("cria o banco de dados alvo quando ele ainda não existe no servidor Postgres")
    void onApplicationEvent_targetDatabaseMissing_createsDatabase() throws Exception {
        String newDbName = "saap_new_db_" + UUID.randomUUID().toString().replace("-", "");
        String targetUrl = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432)
                + "/" + newDbName;

        when(event.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("spring.datasource.url")).thenReturn(targetUrl);
        when(environment.getProperty("spring.datasource.username")).thenReturn(postgres.getUsername());
        when(environment.getProperty("spring.datasource.password")).thenReturn(postgres.getPassword());

        assertThat(databaseExists(newDbName)).isFalse();

        new DatabaseInitializerListener().onApplicationEvent(event);

        assertThat(databaseExists(newDbName)).isTrue();
    }

    private boolean databaseExists(String dbName) throws Exception {
        String baseUrl = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/postgres";
        try (Connection conn = DriverManager.getConnection(baseUrl, postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'")) {
            return rs.next();
        }
    }
}
