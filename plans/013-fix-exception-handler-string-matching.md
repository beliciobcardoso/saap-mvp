# Plan 013: Fix exception handler HTTP status decision based on exception type instead of string match

> **Executor instructions**: Read plan fully. This is a straightforward refactor. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`GlobalExceptionHandler` decides HTTP status (400 vs 409) by string-matching the exception message: `if ("Horário indisponível".equals(ex.getMessage()))`. If the message changes, the wrong status is returned silently. Solution: create typed exceptions (`ScheduleConflictException`) and handle them explicitly.

## Current state

- **File**: `infrastructure/web/exception/GlobalExceptionHandler.java:141`
- **Code**: String matching on exception message for status decision
- **Effect**: Fragile; breaks silently if message text changes

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |

## Scope

**In scope**:
- Create `ScheduleConflictException` in `domain/exception/`
- Find all places throwing with message "Horário indisponível" and throw the typed exception instead
- Add handler for `ScheduleConflictException` returning HTTP 409
- Remove string-matching logic from catch-all handler

**Out of scope**:
- Do NOT create exceptions for every possible error (only known business conflicts)
- Do NOT change response shapes

## Git workflow

- Branch: `refactor/013-exception-types`
- Commit message: `refactor: replace string-matching exception handling with typed exceptions`

## Steps

### Step 1: Create ScheduleConflictException

New file: `domain/exception/ScheduleConflictException.java`:

```java
public class ScheduleConflictException extends RuntimeException {
    public ScheduleConflictException(String message) {
        super(message);
    }
}
```

**Verify**: File created, extends RuntimeException

### Step 2: Find and replace exception throws

Grep for "Horário indisponível":

```bash
grep -rn "Horário indisponível" src/main/java
```

For each match (likely in booking/scheduling use cases), replace:

```java
// OLD
throw new IllegalStateException("Horário indisponível");

// NEW
throw new ScheduleConflictException("Horário indisponível");
```

**Verify**: All occurrences replaced

### Step 3: Add handler to GlobalExceptionHandler

Add method:

```java
@ExceptionHandler(ScheduleConflictException.class)
public ResponseEntity<ErrorResponse> handleScheduleConflict(ScheduleConflictException ex, HttpServletRequest request) {
    logger.warn("Schedule conflict: {}", ex.getMessage());
    
    ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "O horário solicitado não está disponível. Tente outro horário.",
            request.getRequestURI(),
            null
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
}
```

**Verify**: Handler added to GlobalExceptionHandler

### Step 4: Remove string-matching from catch-all

In `GlobalExceptionHandler`, find the old string-matching logic and delete it (around line 141). If there was logic like:

```java
if ("Horário indisponível".equals(ex.getMessage())) {
    return ResponseEntity.status(HttpStatus.CONFLICT)...
}
```

Delete it. The typed handler will catch `ScheduleConflictException` before the generic one runs.

**Verify**: No string-matching logic remains in catch-all

### Step 5: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

- Existing tests that expected "Horário indisponível" errors should still get 409 status (now via typed exception)
- If a test was checking the string, update it to check the typed exception instead

Verification: `./mvnw clean test` all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `ScheduleConflictException` created in `domain/exception/`
- [ ] All "Horário indisponível" throws replaced with `ScheduleConflictException`
- [ ] `GlobalExceptionHandler` has handler for `ScheduleConflictException` returning HTTP 409
- [ ] String-matching logic removed from catch-all handler
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] Grep for "Horário indisponível" in exception handlers returns zero matches (only in the typed exception message)

## STOP conditions

Stop and report if:

- Multiple different "Horário indisponível" cases need different handling (indicate design mismatch, out of scope)
- Test suite fails because it was checking exception message strings

## Maintenance notes

**Future changes**: When adding new business conflict types, create corresponding typed exceptions (e.g., `PatientAlreadyBookedException`) and handlers rather than expanding string matching.

**Deferred**: Refactor other implicit status decisions (if any exist) to use typed exceptions as well.
