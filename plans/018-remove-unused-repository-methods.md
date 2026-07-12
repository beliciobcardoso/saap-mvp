# Plan 018: Remove unused repository port methods to reduce surface area

> **Executor instructions**: Read plan fully. This is dead-code cleanup. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`AppointmentRepository` has `findByStatusAndDateTimeBetweenAndFollowUpSentFalse()` (and 3 other methods) that are never called. They clutter the API, increase maintenance burden, and create confusion. Solution: remove them.

## Current state

- **Port**: `domain/repository/AppointmentRepository.java:18` (approx)
- **Unused methods**: 4 total (exact list in audit)
- **Effect**: Confusing API surface, maintenance burden

## Scope

**In scope**:
- Remove unused method signatures from port interfaces
- Remove implementations from JPA repositories and adapters
- Verify with grep that methods are not called anywhere

**Out of scope**:
- Do NOT remove methods that are actually used (verify first)

## Steps

### Step 1: Verify methods are truly unused

```bash
grep -rn "findByStatusAndDateTimeBetweenAndFollowUpSentFalse\|findById\|findAll" src/main/java/br/com/belloinfo/saap_mvp \
  --include="*.java" | grep -v "repository\|Repository"
```

If grep returns zero matches (no calls outside repository definitions), methods are dead.

**Verify**: Methods are not called

### Step 2: Remove from port

Edit `domain/repository/AppointmentRepository.java`: delete the method signatures.

### Step 3: Remove from JPA and adapter

Edit `infrastructure/persistence/repository/JpaAppointmentRepository.java`: delete implementations.

Edit `infrastructure/persistence/adapter/AppointmentRepositoryAdapter.java`: delete delegations.

**Verify**: All three locations cleaned

### Step 4: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

No new tests. Existing tests should still pass (these methods were never tested or used).

## Done criteria

- [ ] Unused method signatures removed from all locations
- [ ] Grep for method names returns zero matches
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass

## STOP conditions

Stop and report if:

- Grep finds a call to a method you're about to delete (mistake in dead-code analysis)

## Maintenance notes

**Policy**: Before removing a method, always grep for calls to confirm it's unused.
