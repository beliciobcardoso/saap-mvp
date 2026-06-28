package br.com.belloinfo.saap_mvp.infrastructure.security;

import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.valueobject.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.getSecurity().getToken().setSecret("my-secret-key-test-12345678901234567890");
        securityProperties.getSecurity().getToken().setExpiration(3600000L); // 1 hour
        tokenService = new TokenService(securityProperties);
    }

    @Test
    void shouldGenerateValidToken() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setRole(UserRole.ADMIN);

        String token = tokenService.generateToken(user);
        assertNotNull(token);

        String subject = tokenService.validateToken(token);
        assertEquals("test@email.com", subject);
    }

    @Test
    void shouldReturnNullWhenTokenIsInvalid() {
        String invalidToken = "invalid-token-string";
        String subject = tokenService.validateToken(invalidToken);
        assertNull(subject);
    }
}
