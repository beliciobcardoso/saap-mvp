# Plan 019: Replace Thread.sleep in priority tests with injectable Clock for deterministic testing

> **Executor instructions**: Read plan fully. This is a test refactor for flakiness prevention. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: tests
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`PriorityAttendanceIntegrationTest.java:232` uses `Thread.sleep(10)` to force distinct timestamps, a flaky pattern under CI load. Tests that rely on timing can fail randomly. Solution: inject a `Clock` bean and use it to generate predictable timestamps instead of real time.

## Current state

- **File**: `src/test/java/.../PriorityAttendanceIntegrationTest.java:232`
- **Code**: `Thread.sleep(10)` to ensure different timestamps
- **Effect**: Flaky under load; slows tests (sleep time compounds)

## Scope

**In scope**:
- Create a test `Clock` bean (or use Spring's `Clock.fixed()`)
- Inject Clock into use cases that create timestamps
- Update test to control time without sleeping

**Out of scope**:
- Do NOT change business logic (only test infrastructure)
- Do NOT mock `LocalDateTime.now()` (use Clock instead)

## Steps

### Step 1: Check how timestamps are created

In the use case, find where timestamps are assigned (likely `LocalDateTime.now()`).

If it's hardcoded `LocalDateTime.now()`, refactor to inject a `Clock`:

```java
// BEFORE
LocalDateTime checkInTime = LocalDateTime.now();

// AFTER
LocalDateTime checkInTime = LocalDateTime.now(clock);
```

### Step 2: Update use case to accept Clock

In the use case constructor, add:

```java
private final Clock clock;

public SomeUseCase(Repository repo, Clock clock) {
    this.repository = repo;
    this.clock = clock;
}
```

(If Clock is not already injected.)

### Step 3: Update test to provide fixed Clock

In the test setup:

```java
@Bean
public Clock clock() {
    return Clock.fixed(Instant.parse("2026-07-10T12:00:00Z"), ZoneId.systemDefault());
}
```

Remove `Thread.sleep()` and instead advance time in test steps via `clock.instant()` or by creating a new fixed clock per step.

**Verify**: Test no longer uses Thread.sleep()

### Step 4: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass (and run faster)

## Test plan

- Existing test should pass (logic unchanged, just deterministic timestamps)
- Test execution time should be slightly faster (no sleep)

## Done criteria

- [ ] `Thread.sleep()` call removed from test
- [ ] Clock is injected into use case
- [ ] Test uses fixed Clock bean instead
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] Test execution time is faster (no sleep)

## STOP conditions

Stop and report if:

- Clock injection would require major use case refactoring (out of scope, escalate)

## Maintenance notes

**Pattern**: For any test that needs to control time, use a fixed Clock instead of Thread.sleep().
