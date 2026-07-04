package br.com.belloinfo.saap_mvp.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public void blacklist(String token) {
        blacklistedTokens.add(token);
        log.debug("Token adicionado à blacklist: {}...", token.substring(0, Math.min(20, token.length())));
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        int before = blacklistedTokens.size();
        blacklistedTokens.removeIf(token -> {
            try {
                Date expiresAt = JWT.decode(token).getExpiresAt();
                return expiresAt != null && expiresAt.before(new Date());
            } catch (JWTDecodeException e) {
                return true;
            }
        });
        int removed = before - blacklistedTokens.size();
        if (removed > 0) {
            log.info("Blacklist limpa: {} tokens expirados removidos. Restam {} tokens ativos.", removed, blacklistedTokens.size());
        }
    }
}
