# Plan 008: Fix Testcontainers and Jackson version mismatch in pom.xml

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat a5a9a5a..HEAD -- pom.xml`
> If changed, compare the file against the excerpts below before proceeding.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW (pinning to consistent versions, no breaking changes)
- **Depends on**: none
- **Category**: dependencies
- **Planned at**: commit `a5a9a5a`, 2026-07-10

## Why this matters

Two dependency version mismatches threaten the integration test suite:

1. **Testcontainers major-version mismatch**: Pinned modules (`postgresql:1.19.7`, `junit-jupiter:1.19.7`) resolve against transitively managed `testcontainers` core at `2.0.5` — a jump across major versions (1.x → 2.x), which can break the test infrastructure (Postgres container startup, JUnit integration).

2. **Jackson dual-generation conflict**: Both Jackson 2.21.4 (from Spring Boot) and 3.1.4 (from `springdoc-openapi`) coexist on the classpath. Jackson 3 is a breaking major release; dual versions cause serialization/deserialization issues and memory bloat.

Fix: pin all Testcontainers modules to a consistent version, and align Jackson versions explicitly.

## Current state

- **File**: `pom.xml`
- **Issue 1**: Testcontainers `postgresql:1.19.7` + `junit-jupiter:1.19.7` vs transitively resolved `testcontainers:2.0.5` (from transitive deps of those modules)
- **Issue 2**: Spring Boot brings `jackson-databind:2.21.4`, but `springdoc:2.8.5` transitively depends on Jackson 3.x
- **Effect**: Integration tests may fail at container startup; JSON serialization may behave unexpectedly

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Show dependency tree | `./mvnw dependency:tree` | shows all deps, confirms versions |
| Check for Jackson conflicts | `./mvnw dependency:tree \| grep jackson` | lists all jackson artifacts, unified versions |
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |

## Scope

**In scope**:
- `pom.xml` — update Testcontainers module versions and explicitly manage Jackson version

**Out of scope**:
- Do NOT upgrade Spring Boot or springdoc versions (out of scope for this plan)
- Do NOT change test code (only dependency pinning)

## Git workflow

- Branch: `fix/008-dependency-versions`
- Commit message: `fix: align testcontainers and jackson dependency versions`
- Do NOT push unless instructed

## Steps

### Step 1: Check current dependency tree

```bash
./mvnw dependency:tree | grep -E "testcontainers|jackson" | head -20
```

**Verify**: Output shows the conflicting versions as described. Note the exact versions shown.

### Step 2: Pin Testcontainers modules to 1.19.x (stable, matches existing code)

Edit `pom.xml`, locate the `<testcontainers>` dependencies section (lines ~115-130 approx), and ensure all modules use the same version:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.7</version>
    <scope>test</scope>
</dependency>
```

No transitive `testcontainers:2.x` should resolve. If it does, add an exclusion (though this is rare). After editing, verify:

```bash
./mvnw dependency:tree | grep -A 2 "testcontainers:testcontainers"
```

**Verify**: All testcontainers artifacts are at 1.19.7

### Step 3: Explicitly manage Jackson version to 2.x (match Spring Boot)

In `pom.xml`, add a `<dependencyManagement>` section (or add to existing one) to enforce Jackson 2.x:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Force Jackson 2.x to prevent springdoc's 3.x from resolving -->
        <dependency>
            <groupId>com.fasterxml.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>2.21.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

This uses Jackson's BOM (bill of materials), which ensures all Jackson modules use the same version.

**Verify**: `grep -n "jackson-bom" pom.xml` → should find the new BOM import

### Step 4: Exclude Jackson 3.x from springdoc (if still present)

If after step 3 the dependency tree still shows Jackson 3.x artifacts, add an exclusion to the springdoc dependency:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.5</version>
    <exclusions>
        <exclusion>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

(Only if needed; the BOM import in step 3 usually prevents this.)

**Verify**: `./mvnw dependency:tree | grep jackson | grep -v "2.21"` → should return zero matches (no Jackson 3.x)

### Step 5: Compile and test

```bash
./mvnw clean compile
```

**Verify**: BUILD SUCCESS, no dependency resolution errors

```bash
./mvnw clean test
```

**Verify**: All tests pass, including integration tests (Testcontainers startup is clean, no Jackson serialization errors)

### Step 6: Final dependency check

```bash
./mvnw dependency:tree | grep -E "testcontainers|jackson" | sort | uniq
```

**Verify**: All testcontainers at 1.19.7, all Jackson at 2.21.4 (no 3.x artifacts)

## Test plan

Existing integration tests via Testcontainers should pass without modification:
- `BaseIntegrationTest` (and subclasses) should initialize Postgres container cleanly
- JSON serialization/deserialization should work as before (Jackson 2.x stable behavior)
- `./mvnw clean test` should exit 0

No new tests needed. The fix is a dependency configuration change, validated by build success and test pass.

Verification: `./mvnw clean test` all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `pom.xml` has testcontainers `postgresql:1.19.7` and `junit-jupiter:1.19.7`
- [ ] `pom.xml` has `<dependencyManagement>` with `jackson-bom:2.21.4` (or uses Spring Boot's Jackson BOM)
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] `./mvnw dependency:tree | grep jackson | grep "jackson-" | wc -l` shows all artifacts at 2.21.4 (no 3.x artifacts) — output should show only 2.x versions
- [ ] `./mvnw dependency:tree | grep "testcontainers" | grep -v "1.19.7"` returns zero matches (all at 1.19.7)
- [ ] `plans/README.md` status row for plan 008 updated to DONE

## STOP conditions

Stop and report back if:

- `./mvnw dependency:tree` shows `testcontainers:2.x` still resolving after pinning 1.19.7 — this would indicate a transitive dependency pinning to 2.x; add an `<exclusion>` in the offending module
- Jackson BOM import causes a build error (e.g., "BOM not found") — check the version is correct and the module is available in Maven Central
- Tests fail with "Jackson SerializationException" or container startup errors after the fix — this would indicate the versions don't actually resolve the conflict; revert and escalate
- Spring Boot version is newer than 4.1.x and uses Jackson 3.x natively — Spring 6.0+ is moving to Jackson 3.x; check `pom.xml`'s `<parent>` Spring Boot version and align the Jackson BOM to match

## Maintenance notes

**Future changes**: When Spring Boot is upgraded, check the parent POM's default Jackson version and adjust the BOM import accordingly (usually it's inherited automatically if you don't override it).

**For reviewers**: Verify that `springdoc-openapi:2.8.5` is still compatible with Jackson 2.x. If a newer springdoc version comes out that requires Jackson 3.x, a larger migration (upgrading Jackson across the board) may be needed.

**Deferred**: Updating springdoc and Spring Boot to use Jackson 3.x natively is a separate plan (larger scope, more risk).
