package br.com.belloinfo.saap_mvp.infrastructure.security;

public interface TokenBlacklistServiceInterface {

    void blacklist(String token);

    boolean isBlacklisted(String token);

    void cleanupExpiredTokens();
}
