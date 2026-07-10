# Plan 005: Add pessimistic lock to CallNextPatientUseCase to prevent race condition

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat a5a9a5a..HEAD -- src/main/java/br/com/belloinfo/saap_mvp/application/usecase/CallNextPatientUseCase.java src/main/java/br/com/belloinfo/saap_mvp/infrastructure/persistence/repository/JpaAppointmentRepository.java src/main/java/br/com/belloinfo/saap_mvp/infrastructure/persistence/adapter/AppointmentRepositoryAdapter.java`
> If any files changed, compare against the excerpts below before proceeding.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MEDIUM (adds lock semantics, could slow down concurrent calls if not careful)
- **Depends on**: none
- **Category**: correctness
- **Planned at**: commit `a5a9a5a`, 2026-07-10

## Why this matters

`CallNextPatientUseCase` does a check-then-act without a pessimistic lock: it queries for the next patient in the queue, then transitions them to `CALLING` status. If two receptionists call "next patient" at the same time, both may read the same `ARRIVED` appointment, transition it, and try to save it — the second save will fail (optimistic lock exception or duplicate state). `BookAppointmentUseCase` protects against this by explicitly locking the Professional row (`findByIdWithLock`). `CallNextPatientUseCase` should do the same for the Appointment. Fix: replace `findNextInQueue` with a locked variant.

## Current state

- **Files**:
  - `src/main/java/br/com/belloinfo/saap_mvp/application/usecase/CallNextPatientUseCase.java:24-33` — uses plain `findNextInQueue` (no lock)
  - `src/main/java/br/com/belloinfo/saap_mvp/domain/repository/AppointmentRepository.java` — port/interface
  - `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/persistence/repository/JpaAppointmentRepository.java` — Spring Data JpaRepository
- **Effect**: Two concurrent calls can grab the same appointment, causing a conflict on save

### Code excerpt

**CallNextPatientUseCase.java:24-33** (current)
```java
@Transactional
public Appointment execute(UUID professionalId, UUID userId, String ipAddress) {
    ...
    Appointment appointment = appointmentRepository.findNextInQueue(professionalId, startOfDay, endOfDay)
            .orElseThrow(() -> new IllegalStateException("A fila de atendimento está vazia"));

    appointment.transitionTo(AppointmentStatus.CALLING);
    Appointment savedAppointment = appointmentRepository.save(appointment);
    ...
}
```

**BookAppointmentUseCase.java:32-34** (pattern to match)
```java
Professional professional = professionalRepository.findByIdWithLock(professionalId)
        .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));
```

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |
| Find existing lock usage | `grep -rn "@Lock\|LockModeType" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/persistence` | shows `BookAppointmentUseCase`'s pattern |

## Scope

**In scope**:
- `src/main/java/br/com/belloinfo/saap_mvp/domain/repository/AppointmentRepository.java` — add `findNextInQueueWithLock` method
- `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/persistence/repository/JpaAppointmentRepository.java` — implement the locked query
- `src/main/java/br/com/belloinfo/saap_mvp/application/usecase/CallNextPatientUseCase.java` — switch to locked variant
- Test: add a concurrency test `CallNextPatientUseCaseIntegrationTest` (if not already present)

**Out of scope**:
- Do NOT change the state machine or transition logic
- Do NOT change the priority-score calculation
- Do NOT add database indexes (only lock usage)

## Git workflow

- Branch: `fix/005-call-next-patient-lock`
- Commit message: `fix: add pessimistic lock to CallNextPatientUseCase to prevent race condition`
- Do NOT push unless instructed

## Steps

### Step 1: Add findNextInQueueWithLock to AppointmentRepository (port)

Edit `src/main/java/br/com/belloinfo/saap_mvp/domain/repository/AppointmentRepository.java`:

Add a new method signature:

```java
Optional<Appointment> findNextInQueueWithLock(UUID professionalId, LocalDateTime startOfDay, LocalDateTime endOfDay);
```

**Verify**: `grep -n "findNextInQueueWithLock" src/main/java/br/com/belloinfo/saap_mvp/domain/repository/AppointmentRepository.java` → should find the new method signature

### Step 2: Implement findNextInQueueWithLock in JpaAppointmentRepository

Edit `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/persistence/repository/JpaAppointmentRepository.java`:

Add the implementation. Use `@Lock(LockModeType.PESSIMISTIC_WRITE)` to match `BookAppointmentUseCase`'s pattern:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM AppointmentEntity a WHERE a.professional.id = :professionalId " +
       "AND a.status = 'ARRIVED' " +
       "AND a.checkInTime >= :startOfDay AND a.checkInTime < :endOfDay " +
       "ORDER BY a.priorityScore DESC, a.checkInTime ASC " +
       "LIMIT 1")
Optional<AppointmentEntity> findNextInQueueWithLock(
    @Param("professionalId") UUID professionalId,
    @Param("startOfDay") LocalDateTime startOfDay,
    @Param("endOfDay") LocalDateTime endOfDay
);
```

Note: The exact query logic (status, sort order, date range) should match the existing `findNextInQueue` method. Verify by reading that method first.

**Verify**: `grep -n "findNextInQueueWithLock" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/persistence/repository/JpaAppointmentRepository.java` → should find the implementation

### Step 3: Update CallNextPatientUseCase to use the locked variant

Edit `src/main/java/br/com/belloinfo/saap_mvp/application/usecase/CallNextPatientUseCase.java`:

Replace:
```java
Appointment appointment = appointmentRepository.findNextInQueue(professionalId, startOfDay, endOfDay)
```

With:
```java
Appointment appointment = appointmentRepository.findNextInQueueWithLock(professionalId, startOfDay, endOfDay)
```

**Verify**: `grep -n "findNextInQueueWithLock" src/main/java/br/com/belloinfo/saap_mvp/application/usecase/CallNextPatientUseCase.java` → should find the call

### Step 4: Compile and test

```bash
./mvnw clean compile
```

**Verify**: BUILD SUCCESS

```bash
./mvnw clean test
```

**Verify**: All tests pass (existing concurrency tests, if any, should still pass)

## Test plan

- Existing integration tests for `CallNextPatientUseCase` should pass (the lock is transparent to the use case logic)
- New test: `CallNextPatientUseCaseIntegrationTest.shouldPreventConcurrentCallNextPatientRaceCondition()` — spawn two threads that both call `execute()` on the same professional/queue simultaneously, assert that only one succeeds and the other either retries or gets a lock-timeout exception (gracefully handled)
- Pattern: similar to `AppointmentConcurrencyIntegrationTest` (if it exists for `BookAppointmentUseCase`)

Verification: `./mvnw clean test` all pass, including new concurrency test.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `AppointmentRepository` has new method `findNextInQueueWithLock`
- [ ] `JpaAppointmentRepository` implements it with `@Lock(LockModeType.PESSIMISTIC_WRITE)` and correct query
- [ ] `CallNextPatientUseCase.execute()` calls the locked variant
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] `grep -rn "findNextInQueueWithLock" src/main/java` shows exactly 3 hits: interface signature, JPA implementation, use case call
- [ ] `plans/README.md` status row for plan 005 updated to DONE

## STOP conditions

Stop and report back if:

- The existing `findNextInQueue` method uses a different query logic (e.g., different status, sort order, or date field) than what's documented here — copy the exact logic from the original method
- Adding the lock causes test timeouts or deadlocks — this could indicate the schema doesn't have proper indexes on the filtered columns; escalate to the next phase
- The `@Lock` annotation or `LockModeType` is not available — check Spring Data JPA imports and Spring version in pom.xml

## Maintenance notes

**Future changes**: If the priority queue algorithm changes, update both `findNextInQueue` and `findNextInQueueWithLock` identically.

**For reviewers**: Verify that the lock timeout is sensible for the use case (default is usually 30 seconds) — if receptionists are frequently getting lock timeouts, the queue logic or professional load may need optimization.

**Performance note**: Pessimistic locks hold database connections during the transaction. If `CallNextPatientUseCase` has a long execution time (e.g., long audit logging or network calls after acquiring the lock), consider moving those to after the lock is released. Currently, the lock is held until the appointment is saved, which should be quick.
