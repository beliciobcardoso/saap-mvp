# Plan 017: Remove hardcoded database password from docs/ERROS-E-SOLUCOES.md

> **Executor instructions**: Read plan fully. This is a security cleanup. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: security/docs
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`docs/ERROS-E-SOLUCOES.md:45` contains a plaintext database password (`P@ssw0rds`) used for Flyway repair commands during dev setup. While this is a dev-only credential, storing it in checked-in documentation is a security smell. Solution: remove the hardcoded password and reference the env var instead.

## Current state

- **File**: `docs/ERROS-E-SOLUCOES.md` line 45
- **Content**: Flyway repair command with `-Dflyway.password=P@ssw0rds`
- **Effect**: Credential in public repo (low impact since it's dev-only, but sets bad precedent)

## Scope

**In scope**:
- Remove hardcoded password from the documentation
- Replace with placeholder or env var reference

**Out of scope**:
- Do NOT change the Flyway commands or their functionality
- Do NOT delete the entire section (context is useful)

## Steps

### Step 1: Update the documentation

Edit `docs/ERROS-E-SOLUCOES.md`:

Find the Flyway repair command (around line 45) and replace:

```bash
# OLD
./mvnw flyway:repair -Dflyway.url="jdbc:postgresql://localhost:5432/saap_db" \
  -Dflyway.user=postgres -Dflyway.password=P@ssw0rds

# NEW
./mvnw flyway:repair -Dflyway.url="jdbc:postgresql://localhost:5432/saap_db" \
  -Dflyway.user=postgres -Dflyway.password="${DB_PASSWORD:-postgres}"
```

Or, use an env var:

```bash
./mvnw flyway:repair \
  -Dflyway.url="jdbc:postgresql://localhost:5432/saap_db" \
  -Dflyway.user=postgres \
  -Dflyway.password="${DATABASE_PASSWORD}"
```

**Verify**: Hardcoded password removed

### Step 2: Compile and verify (docs-only change)

No compilation needed. Just verify the file reads correctly:

```bash
grep -n "P@ssw0rds" docs/ERROS-E-SOLUCOES.md
```

**Verify**: Returns zero matches (password removed)

## Test plan

No code tests. Manual verification: grep for hardcoded password.

## Done criteria

- [ ] Hardcoded password `P@ssw0rds` not present in docs/ERROS-E-SOLUCOES.md
- [ ] Flyway commands reference env vars or prompts user for password

## STOP conditions

Stop and report if:

- Other hardcoded credentials exist elsewhere in docs/ (document separately)

## Maintenance notes

**Going forward**: Use env vars or `.env` files for all credentials in documentation, never hardcoded values.
