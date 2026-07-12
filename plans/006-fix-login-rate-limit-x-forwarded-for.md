# Plan 006: Fix LoginRateLimitFilter to validate trusted proxies

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat a5a9a5a..HEAD -- src/main/java/br/com/belloinfo/saap_mvp/infrastructure/security/LoginRateLimitFilter.java src/main/resources/application.yaml`
> If changed, compare the files against the excerpts below before proceeding.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MEDIUM (requires proxy config; misconfiguration still allows bypass)
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `a5a9a5a`, 2026-07-10

## Why this matters

`LoginRateLimitFilter.getClientIp()` unconditionally trusts the `X-Forwarded-For` header. An attacker can spoof this header per-request to get a fresh rate-limit bucket and perform unlimited login attempts. Fix: add a configuration list of trusted proxies and extract the real client IP only from trusted forwarding headers.

## Current state

- **File**: `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/security/LoginRateLimitFilter.java:58-64`
- **Effect**: Rate limit is bypassable by rotating `X-Forwarded-For` header per request

### Code excerpt

**LoginRateLimitFilter.java:58-64** (current — vulnerable)
```java
private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
        return xForwardedFor.split(",")[0].trim(); // <-- TRUSTS CLIENT HEADER
    }
    return request.getRemoteAddr();
}
```

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |
| Grep for trusted hosts config | `grep -n "login.*rate.*proxy\|trusted.*host" src/main/resources/application.yaml` | should find new config after edit |

## Scope

**In scope**:
- `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/security/LoginRateLimitFilter.java` — add trusted-proxy validation
- `src/main/resources/application.yaml` — add `api.security.login.trusted-proxies` config list
- `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/config/SaapProperties.java` (or new property class) — add property binding

**Out of scope**:
- Do NOT change the rate-limit algorithm (5 attempts per minute, per client IP)
- Do NOT add new endpoints or change auth flow
- Do NOT implement IP geofencing or advanced bot detection

## Git workflow

- Branch: `fix/006-login-rate-limit-proxy`
- Commit message: `fix: validate trusted proxies in login rate limiter (prevent X-Forwarded-For spoofing)`
- Do NOT push unless instructed

## Steps

### Step 1: Add trusted-proxies configuration to application.yaml

Edit `src/main/resources/application.yaml`:

Add (or merge with existing `api.security` block):

```yaml
api:
  security:
    login:
      trusted-proxies: "${LOGIN_TRUSTED_PROXIES:}"  # Comma-separated IPs or 'all' for dev
      rate-limit-minutes: 1
      max-attempts: 5
```

Leave empty by default (production: no proxies trusted). For development/local testing, users can set `LOGIN_TRUSTED_PROXIES=127.0.0.1,localhost` or use `all` (not recommended for production).

**Verify**: `grep -n "trusted-proxies" src/main/resources/application.yaml` → should find the new config

### Step 2: Add property binding class (if not already present)

Check if `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/config/SaapProperties.java` already has a nested class for login config. If not, add it:

```java
public record SaapProperties(
    // ... existing fields ...
    LoginConfig login
) {
    public record LoginConfig(
        String trustedProxies,
        int rateLimitMinutes,
        int maxAttempts
    ) {}
}
```

Or, if `SaapProperties` is not a record, add a nested class:

```java
@ConfigurationProperties(prefix = "api.security")
public class SecurityProperties {
    private LoginConfig login = new LoginConfig();
    
    public static class LoginConfig {
        private String trustedProxies = "";
        private int rateLimitMinutes = 1;
        private int maxAttempts = 5;
        
        // Getters and setters
        public String getTrustedProxies() { return trustedProxies; }
        public void setTrustedProxies(String trustedProxies) { this.trustedProxies = trustedProxies; }
        // ... etc
    }
}
```

**Verify**: `grep -n "trustedProxies\|LoginConfig" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/config/SaapProperties.java` (or new class location) → should find the property

### Step 3: Update LoginRateLimitFilter to use trusted-proxy validation

Edit `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/security/LoginRateLimitFilter.java`:

Replace `getClientIp()` with:

```java
private String getClientIp(HttpServletRequest request) {
    // Only trust X-Forwarded-For if the immediate peer is a trusted proxy
    String remoteAddr = request.getRemoteAddr();
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    
    // If no trusted proxies configured, never trust X-Forwarded-For
    if (trustedProxies == null || trustedProxies.isEmpty()) {
        return remoteAddr;
    }
    
    // If configured with "all" (dev only), trust any header
    if ("all".equalsIgnoreCase(trustedProxies)) {
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
    } else {
        // Only trust X-Forwarded-For if remoteAddr is in the trusted list
        List<String> trusted = Arrays.asList(trustedProxies.split(","));
        if (trusted.contains(remoteAddr) && xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
    }
    
    return remoteAddr;
}
```

Also, add a field to hold the trusted-proxies config:

```java
private final String trustedProxies;

public LoginRateLimitFilter(SaapProperties saapProperties) { // or inject SecurityProperties
    this.trustedProxies = saapProperties.login().trustedProxies(); // Adjust based on property class name
}
```

**Verify**: `grep -n "trustedProxies\|getClientIp" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/security/LoginRateLimitFilter.java` → should find the field and updated method

### Step 4: Compile and test

```bash
./mvnw clean compile
```

**Verify**: BUILD SUCCESS

```bash
./mvnw clean test
```

**Verify**: All tests pass, including `LoginRateLimitFilterTest` (update test if it was checking the old trusting behavior)

## Test plan

- Update existing `LoginRateLimitFilterTest`:
  - Test case: `testGetClientIpWithoutTrustedProxies()` — X-Forwarded-For is spoofed, should use `remoteAddr` instead
  - Test case: `testGetClientIpWithTrustedProxy()` — remoteAddr is in trusted list, X-Forwarded-For is trusted
  - Test case: `testGetClientIpWithUntrustedProxy()` — remoteAddr is NOT in trusted list, should ignore X-Forwarded-For
  - Test case: `testAllProxiesTrustedInDev()` — trustedProxies = "all", any X-Forwarded-For is used (dev mode)

Verification: `./mvnw clean test` all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `application.yaml` has `api.security.login.trusted-proxies` config with default empty string
- [ ] Property binding class (SaapProperties or SecurityProperties) has `LoginConfig` with `trustedProxies` field
- [ ] `LoginRateLimitFilter.getClientIp()` validates trusted proxies before trusting X-Forwarded-For
- [ ] `LoginRateLimitFilter` constructor injects the trusted-proxies config
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass, including new trusted-proxy test cases
- [ ] `grep -n "X-Forwarded-For" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/security/LoginRateLimitFilter.java | grep -v "trust"` returns zero matches (no blind trust)
- [ ] `plans/README.md` status row for plan 006 updated to DONE

## STOP conditions

Stop and report back if:

- Spring fails to bind the `api.security.login.trusted-proxies` property — check the property-class annotation and prefix match
- Test suite fails because `LoginRateLimitFilterTest` was hardcoded with assumptions about the old trusting behavior — update the test to mock the trusted-proxies config
- The `SaapProperties` class doesn't exist or can't be injected into filters — check if there's a different way to inject configuration in this project (e.g., custom property holder)

## Maintenance notes

**Deployment**: In production, set `LOGIN_TRUSTED_PROXIES` to the exact IP address(es) of the reverse proxy(ies) in front of the app (e.g., `LOGIN_TRUSTED_PROXIES=10.0.0.1,10.0.0.2`). Do NOT use "all" in production.

**For reviewers**: Verify that the proxy IP list is maintained in deployment/ops automation (e.g., Terraform, K8s ConfigMap) so it stays in sync with actual proxy changes.

**Future changes**: If multiple reverse proxies are chained, X-Forwarded-For will have multiple comma-separated IPs. The fix uses `split(",")[0]` (rightmost/closest proxy), which is correct, but validate that this matches your proxy topology.

**Deferred**: Rate-limit persistence across service restarts is not addressed here (stored in-memory Map). If you need rate-limits to survive restarts, use Redis or a database store (follow-up plan).
