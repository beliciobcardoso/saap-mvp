package br.com.belloinfo.saap_mvp.infrastructure.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

/**
 * Cobre apenas os ramos de early-return do listener, sem depender de um
 * banco de dados real. O caminho que efetivamente abre conexão JDBC é
 * exercitado separadamente em {@link DatabaseInitializerListenerIT}, que
 * usa Testcontainers.
 */
@ExtendWith(MockitoExtension.class)
class DatabaseInitializerListenerTest {

    @Mock
    private ApplicationEnvironmentPreparedEvent event;

    @Mock
    private ConfigurableEnvironment environment;

    private final DatabaseInitializerListener listener = new DatabaseInitializerListener();

    @Test
    @DisplayName("não faz nada quando spring.datasource.url é nulo")
    void onApplicationEvent_urlNull_doesNothing() {
        when(event.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("spring.datasource.url")).thenReturn(null);

        assertDoesNotThrow(() -> listener.onApplicationEvent(event));
    }

    @Test
    @DisplayName("não faz nada quando a url não contém 'postgresql'")
    void onApplicationEvent_urlWithoutPostgresql_doesNothing() {
        when(event.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("spring.datasource.url")).thenReturn("jdbc:h2:mem:test");

        assertDoesNotThrow(() -> listener.onApplicationEvent(event));
    }

    @Test
    @DisplayName("não faz nada quando a url contém 'postgresql' mas não possui '/' (lastIndexOf == -1)")
    void onApplicationEvent_urlWithoutSlash_doesNothing() {
        when(event.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("spring.datasource.url")).thenReturn("postgresqlnoslash");

        assertDoesNotThrow(() -> listener.onApplicationEvent(event));
    }

    @Test
    @DisplayName("rejeita nome de banco com caracteres inválidos sem tentar conectar (proteção contra SQL injection)")
    void onApplicationEvent_invalidDbName_rejectsWithoutConnecting() {
        when(event.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("spring.datasource.url"))
                .thenReturn("jdbc:postgresql://localhost:5432/app; DROP TABLE users;--");
        when(environment.getProperty("spring.datasource.username")).thenReturn("user");
        when(environment.getProperty("spring.datasource.password")).thenReturn("pass");

        assertDoesNotThrow(() -> listener.onApplicationEvent(event));
    }
}
