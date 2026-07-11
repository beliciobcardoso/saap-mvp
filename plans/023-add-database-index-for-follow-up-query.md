# Plan 023: Add database index (status, data_hora) for appointment follow-up scheduler query

> **Executor instructions**: Read plan fully. This is a database optimization. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: performance
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`AppointmentFollowUpScheduler` queries appointments with `WHERE status = 'PENDING_RESPONSE' AND data_hora >= ...`. No index covers `(status, data_hora)`, so the query scans the full table every hour. Solution: add a composite index.

## Current state

- **File**: `src/main/resources/db/migration/V3__criar_tabelas_agendamento.sql:18`
- **Query**: Looks for appointments by status and date_hora without index
- **Effect**: Full table scan on scheduler tick, slow if table has 100k+ rows

## Scope

**In scope**:
- Create new Flyway migration (V11) to add the index
- Index: `CREATE INDEX ON agendamento(status, data_hora)`

**Out of scope**:
- Do NOT change the query itself
- Do NOT add other indexes (only this one)

## Steps

### Step 1: Create Flyway migration

New file: `src/main/resources/db/migration/V11__criar_index_agendamento_status_data_hora.sql`:

```sql
-- Index para otimizar queries do scheduler de follow-up
CREATE INDEX IF NOT EXISTS idx_agendamento_status_data_hora ON agendamento(status, data_hora);
```

**Verify**: File created with valid SQL syntax

### Step 2: Compile and verify migration

```bash
./mvnw clean compile
```

**Verify**: Migration is recognized by Flyway (filename V11)

### Step 3: Run app to apply migration

```bash
./mvnw spring-boot:run
```

The migration will apply on boot.

**Verify**: No migration errors in logs

### Step 4: Test

```bash
./mvnw clean test
```

**Verify**: Tests pass

## Test plan

No new tests. Existing integration tests run with the index present.

## Done criteria

- [ ] Migration file `V11__...sql` created in db/migration/
- [ ] Index syntax is correct
- [ ] Migration applies without error
- [ ] `./mvnw clean test` exits 0, all tests pass

## STOP conditions

Stop and report if:

- Migration fails due to syntax error or constraint violation

## Maintenance notes

**Monitoring**: After deploying to production, monitor slow query logs to confirm the index is being used and query time improved.

**Deferred**: Further index optimization (composite indexes on other columns) can be added as a follow-up if profiling shows other hot queries.
