package br.com.belloinfo.saap_mvp.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;

    private static final String SECRET = "test-secret-key-for-jwt-blacklist-testing-12345";

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    private String generateValidToken(long expiresAtMs) {
        Algorithm algorithm = Algorithm.HMAC256(SECRET);
        return JWT.create()
                .withIssuer("saap-api")
                .withSubject("test@email.com")
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiresAtMs))
                .sign(algorithm);
    }

    @Test
    @DisplayName("token recém-adicionado à blacklist é identificado como bloqueado")
    void blacklist_tokenAdded_isBlacklisted() {
        String token = generateValidToken(3600000);

        tokenBlacklistService.blacklist(token);

        assertTrue(tokenBlacklistService.isBlacklisted(token));
    }

    @Test
    @DisplayName("token não adicionado à blacklist não é identificado como bloqueado")
    void isBlacklisted_tokenNotAdded_returnsFalse() {
        String token = generateValidToken(3600000);

        assertFalse(tokenBlacklistService.isBlacklisted(token));
    }

    @Test
    @DisplayName("tokens diferentes na blacklist são independentes")
    void blacklist_differentTokens_areIndependent() {
        String token1 = generateValidToken(3600000);
        String token2 = generateValidToken(3600000);

        tokenBlacklistService.blacklist(token1);

        assertTrue(tokenBlacklistService.isBlacklisted(token1));
        assertFalse(tokenBlacklistService.isBlacklisted(token2));
    }

    @Test
    @DisplayName("cleanup remove tokens expirados da blacklist")
    void cleanupExpiredTokens_removesExpiredTokens() {
        String expiredToken = generateValidToken(-3600000); // expirado 1h atrás
        String validToken = generateValidToken(3600000);   // expira em 1h

        tokenBlacklistService.blacklist(expiredToken);
        tokenBlacklistService.blacklist(validToken);

        tokenBlacklistService.cleanupExpiredTokens();

        assertFalse(tokenBlacklistService.isBlacklisted(expiredToken));
        assertTrue(tokenBlacklistService.isBlacklisted(validToken));
    }

    @Test
    @DisplayName("cleanup remove tokens com formato inválido")
    void cleanupExpiredTokens_removesInvalidTokens() {
        String invalidToken = "invalid-token-format";

        tokenBlacklistService.blacklist(invalidToken);

        tokenBlacklistService.cleanupExpiredTokens();

        assertFalse(tokenBlacklistService.isBlacklisted(invalidToken));
    }

    @Test
    @DisplayName("cleanup não remove tokens válidos não expirados")
    void cleanupExpiredTokens_keepsValidTokens() {
        String validToken = generateValidToken(3600000);

        tokenBlacklistService.blacklist(validToken);

        tokenBlacklistService.cleanupExpiredTokens();

        assertTrue(tokenBlacklistService.isBlacklisted(validToken));
    }

    @Test
    @DisplayName("blacklist aceita múltiplos tokens simultaneamente")
    void blacklist_multipleTokens_allBlacklisted() {
        String token1 = generateValidToken(3600000);
        String token2 = generateValidToken(3600000);
        String token3 = generateValidToken(3600000);

        tokenBlacklistService.blacklist(token1);
        tokenBlacklistService.blacklist(token2);
        tokenBlacklistService.blacklist(token3);

        assertTrue(tokenBlacklistService.isBlacklisted(token1));
        assertTrue(tokenBlacklistService.isBlacklisted(token2));
        assertTrue(tokenBlacklistService.isBlacklisted(token3));
    }
}
