# Plan 011: Add pagination to ListAuditLogsUseCase to prevent unbounded database loads

> **Executor instructions**: Read plan fully. Follow each step. This is straightforward perf optimization. If you hit uncertainty, report it. Do NOT push or commit.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: performance
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`ListAuditLogsUseCase.execute()` calls `userRepository.findAll()` (unbounded) on every page view to resolve user emails. With 10k patients, this fetches all 10k users every time someone views audit logs. Solution: replace with scoped batch fetch — only load users whose IDs appear on the current page.

## Current state

- **File**: `application/usecase/ListAuditLogsUseCase.java:30-31`
- **Code**: `Map<UUID, String> userIdToEmail = userRepository.findAll().stream()...`
- **Effect**: Full user table scanned on every audit log page view

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |

## Scope

**In scope**:
- Replace `userRepository.findAll()` with batch fetch of only user IDs on the current page
- Add `findByIdIn(List<UUID> ids)` method to UserRepository if not present

**Out of scope**:
- Do NOT change the audit log pagination logic itself
- Do NOT change response shapes

## Git workflow

- Branch: `perf/011-audit-logs-pagination`
- Commit message: `perf: optimize ListAuditLogsUseCase to avoid full user table scan`

## Steps

### Step 1: Add findByIdIn method to UserRepository (if not present)

Check if `UserRepository` already has `findByIdIn(List<UUID> ids)`. If not, add:

Edit `domain/repository/UserRepository.java`:

```java
List<User> findByIdIn(List<UUID> ids);
```

Edit `infrastructure/persistence/repository/JpaUserRepository.java`:

```java
@Query("SELECT u FROM UserEntity u WHERE u.id IN :ids")
List<UserEntity> findByIdIn(@Param("ids") List<UUID> ids);
```

**Verify**: Method exists in both port and JPA implementation

### Step 2: Update ListAuditLogsUseCase

Edit `application/usecase/ListAuditLogsUseCase.java`:

Replace:
```java
Map<UUID, String> userIdToEmail = userRepository.findAll().stream()
        .collect(Collectors.toMap(User::getId, User::getEmail, (e1, e2) -> e1));
```

With:
```java
// Extract only the user IDs from the current page of audit logs
List<UUID> userIds = auditLogs.stream()
        .map(AuditLog::getUserId)
        .distinct()
        .collect(Collectors.toList());

// Load only those users (batch fetch, not full table scan)
Map<UUID, String> userIdToEmail = userRepository.findByIdIn(userIds).stream()
        .collect(Collectors.toMap(User::getId, User::getEmail, (e1, e2) -> e1));
```

**Verify**: Code changed, compile succeeds

### Step 3: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

- Existing ListAuditLogsUseCase tests should still pass (logic unchanged, just more efficient)
- Optional: add performance test comparing old (full scan) vs new (batch fetch) execution time

Verification: `./mvnw clean test` all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `UserRepository` has `findByIdIn(List<UUID> ids)` method
- [ ] `JpaUserRepository` implements it with @Query
- [ ] `ListAuditLogsUseCase.execute()` calls `findByIdIn()` instead of `findAll()`
- [ ] User ID extraction from audit logs happens before the batch fetch
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] No `findAll()` call for users in ListAuditLogsUseCase

## STOP conditions

Stop and report if:

- UserRepository doesn't support query methods (architecture issue)
- Test suite fails because a test expected the full user list to be loaded

## Maintenance notes

**Future changes**: If audit logs display expands to show more user details, this batch fetch may need to load more fields (e.g. user.name in addition to email). Update the query/mapping accordingly.

**Performance note**: This change also benefits the database directly — fewer rows scanned, better query plan.
