# Plan 004: Fix generic exception handler leaking raw error messages

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat a5a9a5a..HEAD -- src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandler.java`
> If changed, compare the file against the excerpts below before proceeding.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `a5a9a5a`, 2026-07-10

## Why this matters

`GlobalExceptionHandler.handleGenericException()` (catch-all for uncaught `Exception`) returns `ex.getMessage()` directly in the API response body. This leaks sensitive details: SQL syntax errors, null-pointer exceptions, stack traces, internal file paths. Production APIs should return a generic "Internal Server Error" message and log the real error server-side. Fix: sanitize the catch-all handler.

## Current state

- **File**: `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandler.java:156-167`
- **Effect**: Any unhandled exception → HTTP 500 with raw error message in response body

### Code excerpt

**GlobalExceptionHandler.java:156-167** (current)
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            ex.getMessage(),  // <-- LEAKS SENSITIVE DETAIL
            request.getRequestURI(),
            null
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
}
```

Also noted: no handler for `ObjectOptimisticLockingFailureException`, which also falls through to this catch-all and returns 500 instead of 409.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |
| Grep for message leaks | `grep -n "ex.getMessage()" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandler.java` | should find zero occurrences after edit |

## Scope

**In scope**:
- `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandler.java` — fix `handleGenericException` and add handler for `ObjectOptimisticLockingFailureException`
- Add structured logging of the real exception server-side (SLF4J logger)

**Out of scope**:
- Do NOT change ErrorResponse record structure
- Do NOT change HTTP status of other handlers
- Do NOT add new exception types (only handle existing ones better)

## Git workflow

- Branch: `fix/004-generic-exception-handler`
- Commit message: `fix: sanitize generic exception handler to prevent error message leakage`
- Do NOT push unless instructed

## Steps

### Step 1: Add a logger and sanitize handleGenericException

Edit `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandler.java`:

At the top of the class, add:

```java
private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
```

Then replace the `handleGenericException` method:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    // Log the full exception server-side for debugging
    logger.error("Unhandled exception", ex);
    
    // Return a generic message to the client (do NOT leak ex.getMessage())
    ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            null, // No detailed message — safe for production
            request.getRequestURI(),
            null
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
}
```

**Verify**: `grep -n "ex.getMessage()" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandler.java` → should return zero matches

### Step 2: Add handler for ObjectOptimisticLockingFailureException (409 Conflict)

Add a new handler method to the same class:

```java
@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<ErrorResponse> handleOptimisticLockingException(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
    logger.warn("Optimistic lock conflict: {}", ex.getMessage());
    
    ErrorResponse errorResponse = new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(), // 409
            "Conflict",
            "The resource was modified by another request. Please retry.",
            request.getRequestURI(),
            null
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
}
```

You'll need to import: `org.springframework.orm.ObjectOptimisticLockingFailureException`

**Verify**: `grep -n "handleOptimisticLockingException" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandler.java` → should find the new method

### Step 3: Compile and test

```bash
./mvnw clean compile
```

**Verify**: BUILD SUCCESS, no import errors

```bash
./mvnw clean test
```

**Verify**: All tests pass

## Test plan

- Existing tests for exception handlers should still pass (they test HTTP status and structure, not the message content)
- New test: `GlobalExceptionHandlerTest.shouldNotLeakErrorDetailsInGenericHandler()` — mock any uncaught exception, invoke the handler, assert the response body `error` field is `null` or a generic message (not the raw exception message)
- New test: `GlobalExceptionHandlerTest.shouldReturn409ForOptimisticLockException()` — mock `ObjectOptimisticLockingFailureException`, assert HTTP 409

Model after existing test structure in `src/test/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandlerTest.java` (if it exists).

Verification: `./mvnw clean test` all pass, including new tests.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `GlobalExceptionHandler.handleGenericException()` no longer includes `ex.getMessage()` in the response body (use `null` or a safe generic message)
- [ ] `GlobalExceptionHandler` has a new `handleOptimisticLockingException()` method that returns HTTP 409
- [ ] Logger is added at class level (`private static final Logger logger = ...`)
- [ ] All logger calls use safe patterns (`logger.error(..., ex)` or `logger.warn("...", ex)`, not `logger.error(ex.toString())`)
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] `grep -n "ex.getMessage()" src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/exception/GlobalExceptionHandler.java` returns zero matches
- [ ] `plans/README.md` status row for plan 004 updated to DONE

## STOP conditions

Stop and report back if:

- Test suite fails because existing tests were checking the old `ex.getMessage()` in response bodies — update those tests to assert `null` or a generic message instead
- The `ObjectOptimisticLockingFailureException` class is not available (wrong Spring Boot import) — check pom.xml version and the correct import path
- A test or integration test was relying on leaking error messages to diagnose issues — add a log statement to the handler instead so the message is server-side only

## Maintenance notes

**Future changes**: If more specific exception types need custom handling (e.g., `DataAccessException` subtypes), add handlers before the catch-all to avoid falling through to the generic handler.

**For reviewers**: Verify that logging configuration (`logback-spring.xml`) is set to save ERROR-level logs for debugging. If logs are not persisted, the server-side logging adds no value.

**Logging best practice**: Consider adding a correlation/trace ID to logs so support can match a client-reported error with server-side logs (see Plan 022 on structured logging).
