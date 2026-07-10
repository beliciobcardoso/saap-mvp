# Plan 002: Gate Swagger/OpenAPI documentation by profile

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat a5a9a5a..HEAD -- src/main/java/br/com/belloinfo/saap_mvp/infrastructure/security/SecurityConfig.java src/main/java/br/com/belloinfo/saap_mvp/infrastructure/config/OpenApiConfig.java`
> If either file changed, compare the excerpts below against the live code before proceeding.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `a5a9a5a`, 2026-07-10

## Why this matters

`SecurityConfig.java` unconditionally permits `/swagger-ui/**` and `/v3/api-docs/**` for all requests (`permitAll()`), while `OpenApiConfig` is marked `@Profile("dev")` to gate the Spring-managed OpenAPI beans. Problem: springdoc-openapi auto-serves the endpoints even without the `OpenApiConfig` bean, so the UI is publicly accessible in production. Solution: add `springdoc.swagger-ui.enabled=false` and `springdoc.api-docs.enabled=false` to `application.yaml` defaults, override to `true` only in `application-dev.yaml`.

## Current state

- **Files**:
  - `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/security/SecurityConfig.java:66-67` — unconditional `permitAll()` for swagger
  - `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/config/OpenApiConfig.java:17-19` — `@Profile("dev")`
  - `src/main/resources/application.yaml` — no `springdoc.*` properties
  - `src/main/resources/application-dev.yaml` — does not exist yet
- **Effect**: Swagger UI accessible in all environments

### Code excerpts

**SecurityConfig.java:66-67**
```java
.requestMatchers("/swagger-ui/**").permitAll()
.requestMatchers("/v3/api-docs/**").permitAll()
```

**OpenApiConfig.java:17-19**
```java
@Configuration
@Profile("dev")
public class OpenApiConfig {
```

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Check existing profiles | `ls src/main/resources/application-*.yaml` | lists existing profile files |
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |
| Search for springdoc config | `grep -n "springdoc" src/main/resources/application*.yaml` | no matches (baseline) |

## Scope

**In scope**:
- `src/main/resources/application.yaml` — add default `springdoc.swagger-ui.enabled: false` and `springdoc.api-docs.enabled: false`
- `src/main/resources/application-dev.yaml` — create new file, override both to `true`
- No code changes to Java files

**Out of scope**:
- Do NOT change `SecurityConfig` or `OpenApiConfig` class code
- Do NOT remove the `permitAll()` rules (they are harmless once the endpoints are disabled via properties)

## Git workflow

- Branch: `fix/002-gate-swagger-by-profile`
- Commit message: `fix: gate swagger-ui and api-docs endpoints by profile (disabled by default, enabled in dev)`
- Do NOT push unless instructed

## Steps

### Step 1: Add springdoc disabling to application.yaml

Edit `src/main/resources/application.yaml` and add the following lines at the end (or under a new `springdoc:` section if one exists):

```yaml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

**Verify**: `grep -A 2 "springdoc:" src/main/resources/application.yaml` → should show both `enabled: false` lines

### Step 2: Create application-dev.yaml with springdoc enabled

Create `src/main/resources/application-dev.yaml` with:

```yaml
# Development profile — enables Swagger UI and OpenAPI docs
springdoc:
  swagger-ui:
    enabled: true
  api-docs:
    enabled: true
```

**Verify**: `test -f src/main/resources/application-dev.yaml && echo "file created"` → prints "file created"

### Step 3: Compile and verify no errors

```bash
./mvnw clean compile
```

**Verify**: `grep "BUILD SUCCESS"` in output → BUILD SUCCESS with no warnings about unknown springdoc properties

### Step 4: Run tests with default profile (swagger disabled)

```bash
./mvnw clean test -Dspring.profiles.active=
```

This runs tests without the `dev` profile. Verify no errors from missing Swagger endpoints.

**Verify**: All tests pass (exit code 0)

### Step 5: Run a smoke test with dev profile (swagger enabled)

No explicit test needed, but verify the property is parsed:

```bash
./mvnw spring-boot:run -Dspring.boot.run.arguments="--spring.profiles.active=dev" &
sleep 5
curl -s http://localhost:8080/swagger-ui.html | head -c 100 && echo "..."
kill %1
```

**Verify**: The curl returns HTML (Swagger page), not 404. Kill the bg process.

Alternatively, just compile with profile-aware config validation:

```bash
./mvnw clean compile -Dspring.profiles.active=dev
```

**Verify**: BUILD SUCCESS

## Test plan

Existing integration tests run against the default profile (springdoc disabled). Verify:
- No tests expect `/swagger-ui/**` or `/v3/api-docs/**` to be accessible (grep for these paths in test files)
- `./mvnw clean test` exits 0

No new tests needed. The property-based gating is straightforward and validated by Spring Boot's config binding.

**Verification**: `./mvnw clean test` all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `src/main/resources/application.yaml` contains `springdoc.swagger-ui.enabled: false` and `springdoc.api-docs.enabled: false`
- [ ] `src/main/resources/application-dev.yaml` exists and contains both properties set to `true`
- [ ] `./mvnw clean compile` exits 0 (no property errors)
- [ ] `./mvnw clean test` exits 0 (all tests pass)
- [ ] `grep -rn "/swagger-ui" src/test` returns no test assertions expecting swagger to be open (or if present, they are in `*DevTest` or similar dev-only test files)
- [ ] `git status` shows only `application.yaml` (modified) and `application-dev.yaml` (new)
- [ ] `plans/README.md` status row for plan 002 updated to DONE

## STOP conditions

Stop and report back if:

- Spring Boot startup fails with "Unknown property 'springdoc.swagger-ui.enabled'" — indicates a version mismatch with springdoc (check `pom.xml` dependency version)
- A test fails because it expects Swagger to be available in the default test profile — this would indicate the test setup is incomplete, not a plan issue
- The `application-dev.yaml` file is not being loaded in dev environment (test by running with `--spring.profiles.active=dev` and checking logs for "Spring profiles active: dev")

## Maintenance notes

**Future changes**: If springdoc configuration grows (e.g., OpenAPI version, servers), add those properties to both `application.yaml` (sensible dev default) and `application-dev.yaml` (overrides if needed).

**For reviewers**: Confirm that the dev environment documentation (README, CLAUDE.md) mentions running with `--spring.profiles.active=dev` to access Swagger UI. If not, that should be a separate doc update.

**Profile inheritance**: Spring does NOT merge profiles by default. If running with `--spring.profiles.active=dev`, only `application-dev.yaml` is loaded, not `application.yaml`. Verify by checking Spring Boot logs: "Loaded config ... application-dev.yaml". If you want shared properties, keep them in `application.yaml` (no-profile), which always loads first.

**Deferred**: CORS config for Swagger/OpenAPI calls (if CORS is needed for the docs to function) is out of scope here.
