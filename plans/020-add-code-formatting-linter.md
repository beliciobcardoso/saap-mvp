# Plan 020: Add Spotless or Checkstyle for automated code formatting and linting

> **Executor instructions**: Read plan fully. This is a DX improvement. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx/tools
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

No lint/formatter configured. Code style is inconsistent. CI/CD has no gate to enforce style. Solution: add Spotless (via Maven) for auto-formatting on save, and configure it to run as a pre-commit check in CI.

## Scope

**In scope**:
- Add spotless-maven-plugin to pom.xml
- Create `.spotlessrc` or spotless config in pom.xml
- Add verify step to CI workflow
- Optional: add pre-commit hook to format on local commit

**Out of scope**:
- Do NOT enforce strict style requirements that differ from existing code

## Steps

### Step 1: Add spotless to pom.xml

In `<plugins>` section:

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.41.1</version>
    <configuration>
        <java>
            <googleJavaFormat/>
        </java>
    </configuration>
</plugin>
```

**Verify**: pom.xml updated, compile succeeds

### Step 2: Run spotless and apply formatting

```bash
./mvnw spotless:apply
```

This formats all Java files in place.

**Verify**: Files reformatted, git status shows changes

### Step 3: Add spotless check to CI

Edit `.github/workflows/test.yml` and add a step before tests:

```yaml
- name: Check code formatting
  run: ./mvnw spotless:check
```

This will fail the build if files are not formatted.

**Verify**: Workflow updated

### Step 4: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS (after spotless:apply has formatted everything)

## Test plan

No code tests. Verification: spotless:check passes.

## Done criteria

- [ ] spotless-maven-plugin added to pom.xml
- [ ] `./mvnw spotless:apply` has been run and files are formatted
- [ ] `./mvnw spotless:check` exits 0
- [ ] `.github/workflows/test.yml` includes spotless:check step
- [ ] `./mvnw clean compile && ./mvnw clean test` exits 0

## STOP conditions

Stop and report if:

- spotless formatting creates conflicts with existing code style

## Maintenance notes

**For reviewers**: All future PRs will be auto-formatted. Local developers should run `./mvnw spotless:apply` before committing.
