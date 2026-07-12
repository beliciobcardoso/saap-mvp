# Plan 010: Add Redis-backed token blacklist service for production

> **Executor instructions**: Read plan fully. This is a mid-term architectural change. Steps are high-level; implementation may require some design judgment. Follow the scope carefully. If you hit uncertainty, report it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: L
- **Risk**: MEDIUM (introduces Redis dependency, requires deployment config)
- **Depends on**: none
- **Category**: security/architecture
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`TokenBlacklistService` stores revoked tokens in an in-memory `ConcurrentHashMap`. On service restart, all logout tokens are forgotten — users' old tokens suddenly work again. In multi-instance deployments, each instance has its own memory, so logging out in one instance doesn't revoke the token in others. Solution: back the blacklist with Redis for persistence and shared state.

## Current state

- **File**: `infrastructure/security/TokenBlacklistService.java`
- **Implementation**: In-memory only, no persistence
- **Effect**: Logout is unreliable in production

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |

## Scope

**In scope**:
- Add Spring Data Redis dependency to pom.xml
- Create RedisTokenBlacklistService extending/replacing TokenBlacklistService
- Keep in-memory fallback for dev/test (no Redis required locally)
- Use `@ConditionalOnProperty` or profile to switch implementations

**Out of scope**:
- Do NOT change TokenBlacklistService API/interface
- Do NOT add Redis for other purposes (caching, sessions) — only token blacklist
- Do NOT require Redis in dev/test environments

## Git workflow

- Branch: `feat/010-redis-token-blacklist`
- Commit message: `feat: add redis-backed token blacklist for production multi-instance deployments`

## Steps

### Step 1: Add spring-boot-starter-data-redis to pom.xml

In `pom.xml`, locate `<dependencies>` and add:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>io.lettuce</groupId>
    <artifactId>lettuce-core</artifactId>
</dependency>
```

**Verify**: Dependencies added, `./mvnw clean compile` still succeeds

### Step 2: Create RedisTokenBlacklistService

New file: `infrastructure/security/RedisTokenBlacklistService.java`

```java
@Service
@ConditionalOnProperty(name = "api.security.redis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "token:blacklist:";

    @Override
    public void blacklistToken(String token, long expirationTimeInMs) {
        String key = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(expirationTimeInMs));
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        String key = KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
```

Rename the old in-memory service to `InMemoryTokenBlacklistService` and add `@ConditionalOnProperty(name = "api.security.redis.enabled", havingValue = "false")` to keep it as a fallback.

**Verify**: Classes created, compile succeeds

### Step 3: Add Redis config to application.yaml

```yaml
spring:
  redis:
    host: "${REDIS_HOST:localhost}"
    port: "${REDIS_PORT:6379}"
    password: "${REDIS_PASSWORD:}"
    database: 0
    timeout: 2000

api:
  security:
    redis:
      enabled: "${REDIS_ENABLED:false}"  # Default: use in-memory (dev/test)
```

**Verify**: Config added

### Step 4: Update tests

Tests use in-memory by default (REDIS_ENABLED=false). For integration tests that need to verify the service works, either:
- Use a Redis test container (add Testcontainers Redis module), or
- Mock RedisTemplate in unit tests

For now, keep existing tests running with in-memory fallback.

**Verify**: `./mvnw clean test` still passes

### Step 5: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

- Existing TokenBlacklistService tests continue to work (in-memory fallback in test profile)
- Optional: add integration test for RedisTokenBlacklistService with Testcontainers Redis
- Verify ConditionalOnProperty logic: when REDIS_ENABLED=true, RedisTokenBlacklistService is used

Verification: `./mvnw clean test` all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `spring-boot-starter-data-redis` and `lettuce-core` added to pom.xml
- [ ] `RedisTokenBlacklistService` created with @ConditionalOnProperty
- [ ] In-memory service renamed to `InMemoryTokenBlacklistService` with @ConditionalOnProperty(havingValue="false")
- [ ] Both implement the same `TokenBlacklistService` interface
- [ ] `application.yaml` has spring.redis and api.security.redis config
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] TokenBlacklistService API unchanged (no breaking changes to callers)

## STOP conditions

Stop and report if:

- RedisTemplate injection fails (wrong Spring Boot version or missing starter)
- Test suite fails because Redis is unavailable (should use in-memory fallback instead)
- The two implementations (Redis vs in-memory) don't match the interface exactly

## Maintenance notes

**Deployment**: In production, set `REDIS_ENABLED=true` and provide REDIS_HOST/REDIS_PORT. For dev/test, leave REDIS_ENABLED=false (uses in-memory).

**For reviewers**: Verify that the ConditionalOnProperty setup actually switches between implementations as expected. The in-memory service should be the default fallback.

**Deferred**: Redis Sentinel setup for HA is not in scope here (production ops concern, not code).
