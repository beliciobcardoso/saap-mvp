package br.com.belloinfo.saap_mvp.application.service;

import br.com.belloinfo.saap_mvp.infrastructure.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentActionTokenServiceTest {

    private SecurityProperties properties;
    private AppointmentActionTokenService tokenService;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        properties.getSecurity().getToken().setSecret("super-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256");
        properties.getSecurity().getActionToken().setSecret("super-action-token-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-256");
        tokenService = new AppointmentActionTokenService(properties);
    }

    @Test
    void shouldGenerateAndValidateTokenSuccessfully() {
        UUID appointmentId = UUID.randomUUID();
        String token = tokenService.generateToken(appointmentId, "confirm");

        assertNotNull(token);

        AppointmentActionTokenService.DecodedToken decoded = tokenService.validateToken(token);
        assertEquals(appointmentId, decoded.appointmentId());
        assertEquals("confirm", decoded.action());
    }

    @Test
    void shouldFailForTamperedToken() {
        UUID appointmentId = UUID.randomUUID();
        String token = tokenService.generateToken(appointmentId, "cancel");

        String tamperedToken = token + "adulterado";

        assertThrows(IllegalArgumentException.class, () -> tokenService.validateToken(tamperedToken));
    }

    @Test
    void shouldFailForInvalidSecretKey() {
        UUID appointmentId = UUID.randomUUID();
        String token = tokenService.generateToken(appointmentId, "confirm");

        // Set different secret on properties
        properties.getSecurity().getActionToken().setSecret("another-different-key-that-is-at-least-256-bits-long-for-hmac-sha-256");

        assertThrows(IllegalArgumentException.class, () -> tokenService.validateToken(token));
    }
}
