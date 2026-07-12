# Plan 015: Extract getAuthenticatedUserEmail helper to avoid duplication across controllers

> **Executor instructions**: Read plan fully. This is a straightforward deduplication refactor. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`getAuthenticatedUserEmail()` is copy-pasted identically across 6 controllers (PatientController, ProfessionalController, UserController, etc.). Each controller has the same method. Solution: extract to a shared utility or base class.

## Current state

- **Files**: 6 controllers in `infrastructure/web/controller/`
- **Code**: Identical `getAuthenticatedUserEmail()` method in each
- **Effect**: Maintenance burden; fix in one place doesn't propagate; inconsistent refactoring risk

## Scope

**In scope**:
- Create `SecurityUtils` utility class with `getAuthenticatedUserEmail()`
- Replace all 6 duplicate implementations with static calls to the utility
- Ensure the utility handles null auth context gracefully (fallback to "anonymous@saap.com")

**Out of scope**:
- Do NOT change the logic itself (just move it)
- Do NOT change how controllers call it

## Steps

### Step 1: Create SecurityUtils

New file: `infrastructure/security/SecurityUtils.java`:

```java
public final class SecurityUtils {
    private SecurityUtils() {}
    
    public static String getAuthenticatedUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous@saap.com";
    }
}
```

**Verify**: File created, no syntax errors

### Step 2: Replace duplicates in controllers

In each of the 6 controllers, delete the `getAuthenticatedUserEmail()` method and replace calls with `SecurityUtils.getAuthenticatedUserEmail()`.

Example:
```java
// BEFORE
private String getAuthenticatedUserEmail() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return (auth != null) ? auth.getName() : "anonymous@saap.com";
}

// AFTER
// (remove the method)
String email = SecurityUtils.getAuthenticatedUserEmail();  // <-- use utility
```

**Verify**: All 6 controllers updated, no method defined locally anymore

### Step 3: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

- Existing tests should pass (logic unchanged)
- Optional: add a test in SecurityUtilsTest to verify the utility behavior

Verification: `./mvnw clean test` all pass.

## Done criteria

- [ ] `SecurityUtils` class created with `getAuthenticatedUserEmail()`
- [ ] All 6 controllers updated to call `SecurityUtils.getAuthenticatedUserEmail()`
- [ ] No local `getAuthenticatedUserEmail()` methods in controllers (grep returns zero in controller/ dir)
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass

## STOP conditions

Stop and report if:

- One of the controller implementations differs from others (edge case)

## Maintenance notes

**Future additions**: If more controller utilities are needed, expand SecurityUtils or create a ControllerUtils class.
