package br.com.belloinfo.saap_mvp.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "api.security.redis.enabled", havingValue = "true")
public class RedisTokenBlacklistService implements TokenBlacklistServiceInterface {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenBlacklistService.class);
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final long CLEANUP_INTERVAL = 3600000L; // 1 hour in milliseconds

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisTokenBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String token) {
        try {
            Date expirationDate = JWT.decode(token).getExpiresAt();
            if (expirationDate != null) {
                long ttl = expirationDate.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "true", ttl, TimeUnit.MILLISECONDS);
                }
            }
        } catch (JWTDecodeException e) {
            log.warn("Failed to decode token for blacklisting", e);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    @Override
    @Scheduled(fixedDelay = CLEANUP_INTERVAL)
    public void cleanupExpiredTokens() {
        // Redis automatically removes expired keys based on TTL
        log.debug("Cleanup executed for Redis token blacklist (automatic expiration)");
    }
}
