# Plan 001: Remove hardcoded ADMIN credential from V10 migration

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat a5a9a5a..HEAD -- src/main/resources/db/migration/V10__inserir_usuario_admin_teste.sql`
> If the file changed since this plan was written, read it and compare against the "Current state" excerpt below before proceeding.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `a5a9a5a`, 2026-07-10

## Why this matters

`V10__inserir_usuario_admin_teste.sql` inserts an ADMIN user with hardcoded bcrypt hash + plaintext credentials documented in comments. Flyway migrations run on every environment startup (no `application-prod.yaml` gates it), so this credential reaches production on first boot — full account takeover vector. Solution: remove the migration, provide a separate manual setup script for dev environments only.

## Current state

- **File**: `src/main/resources/db/migration/V10__inserir_usuario_admin_teste.sql` — contains `INSERT INTO usuario (...)` with hardcoded ADMIN role + bcrypt hash
- **Flyway**: spring.flyway.enabled defaults to `true`, no profile-specific disabling exists
- **Effect**: credential persists in schema on every environment, including production

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Verify file exists | `ls -l src/main/resources/db/migration/V10__inserir_usuario_admin_teste.sql` | file listed |
| Delete migration | `rm src/main/resources/db/migration/V10__inserir_usuario_admin_teste.sql` | file removed, exit 0 |
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test (full suite) | `./mvnw clean test` | all pass |

## Scope

**In scope**:
- Delete `src/main/resources/db/migration/V10__inserir_usuario_admin_teste.sql`
- Create `docs/dev-setup/admin-user-setup.sql` (new file, manual setup script with instructions not to run in prod)

**Out of scope**:
- Do NOT create any code to auto-generate admin users at runtime
- Do NOT add a separate Java code path for admin initialization
- Do NOT modify other migration files

## Git workflow

- Branch: `fix/001-remove-hardcoded-admin`
- Commit style: match repo convention (`fix: remove hardcoded admin credential from V10 migration` — see recent commits via `git log --oneline | head -5`)
- Do NOT push unless instructed

## Steps

### Step 1: Delete the migration file

Remove `V10__inserir_usuario_admin_teste.sql` entirely. Flyway validates the sequence on startup; since we're removing the highest-numbered migration, the sequence V1-V9 remains valid.

```bash
rm src/main/resources/db/migration/V10__inserir_usuario_admin_teste.sql
```

**Verify**: `ls src/main/resources/db/migration/ | grep -c V10` → should print `0` (no V10 file)

### Step 2: Create a manual admin-setup script for developers

Create `docs/dev-setup/admin-user-setup.sql` with instructions and the same credential data, but clearly marked as dev-only, with instructions to never run in production.

Create the directory first:
```bash
mkdir -p docs/dev-setup
```

Then create `docs/dev-setup/admin-user-setup.sql` with this content:

```sql
-- 🚨 DEVELOPMENT ONLY — NEVER RUN THIS IN PRODUCTION 🚨
-- This script creates a test ADMIN user for local development.
-- Run this manually after creating the database, if needed:
--   psql -U postgres -d saap_db -f docs/dev-setup/admin-user-setup.sql
-- Then immediately CHANGE the password in production before deploying.

INSERT INTO usuario (id, nome, email, cpf, senha, is_active, role, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Admin Teste',
  'admin@test.saap.local',
  '00000000000',
  '[bcrypt hash here — generate fresh for your dev env]',
  true,
  'ADMIN',
  now(),
  now()
)
ON CONFLICT DO NOTHING;
```

**Verify**: `test -f docs/dev-setup/admin-user-setup.sql && echo "file created"` → prints "file created"

### Step 3: Compile and verify no migration sequence errors

Flyway validation happens at app startup. Run:

```bash
./mvnw clean compile
```

**Verify**: `grep -c "BUILD SUCCESS"` in output → should see "BUILD SUCCESS" and no Flyway errors. No errors about missing V9→V10 gap.

### Step 4: Run test suite

```bash
./mvnw clean test
```

**Verify**: All tests pass. No failures due to missing V10 migration.

### Step 5: Verify the migration is truly gone

```bash
find src/main/resources/db/migration -name "V10*"
```

**Verify**: Returns empty (no matches)

## Test plan

No new tests needed. Existing test suite (via Testcontainers) will validate that:
- Flyway migrations V1-V9 apply without error
- No user record pre-seeded by V10 exists
- Manual `admin-user-setup.sql` can be run independently (verification: read the script for SQL syntax correctness, no special invocation needed)

Verification: `./mvnw clean test` exits 0.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `src/main/resources/db/migration/V10__inserir_usuario_admin_teste.sql` does not exist (`find ... -name "V10*"` returns no results)
- [ ] `docs/dev-setup/admin-user-setup.sql` exists and contains "DEVELOPMENT ONLY" warning
- [ ] `./mvnw clean compile` exits 0, no Flyway errors
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] `git status` shows only modified/deleted `V10`, created `docs/dev-setup/admin-user-setup.sql`, and branch files
- [ ] `plans/README.md` status row for plan 001 updated to DONE

## STOP conditions

Stop and report back if:

- Flyway validation fails after deleting V10 (e.g., "missing V9" or checksum mismatch) — this would indicate the migration record is still in the Postgres `flyway_schema_history` table and needs manual cleanup
- Test suite fails after deleting V10 — indicates an integration test was explicitly depending on the V10 user being present
- The file `src/main/resources/db/migration/V10__inserir_usuario_admin_teste.sql` still contains a plaintext password or bcrypt hash in your repo after cleanup (should not happen, but double-check `git diff HEAD`)

## Maintenance notes

**Future changes**: If anyone needs to add a new Flyway migration, the next file should be `V11__...` (not V10, which is now deleted).

**For reviewers**: Confirm that any integration tests that were using the V10-seeded admin user have either:
1. Been updated to create their own test user, or
2. Should have been (if a test is silently relying on V10 and didn't fail, the test may be under-specified)

**Deferred**: Credential rotation in any environment that has already run V10 is the operator's responsibility (this plan only prevents *future* deployments from seeding the credential).
