# Plan 012: Fix CoreMapper asymmetric medical record entries mapping (prevent lazy-load bloat)

> **Executor instructions**: Read plan fully. This is a MapStruct optimization. Follow each step. If mapper syntax is unclear, grep existing mappings in the file as examples. Do NOT push or commit.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: performance
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`CoreMapper.toDomain(MedicalRecordEntity)` auto-maps the lazy `@OneToMany entries` collection from entity to domain, loading the entire clinical history on every read. Meanwhile, `toEntity(MedicalRecord)` explicitly ignores `entries` with `@Mapping(target="entries", ignore=true)`. Asymmetric mapping → unnecessary N+1 or full-collection loads. Solution: explicitly ignore `entries` in the domain-direction mapping too.

## Current state

- **File**: `infrastructure/persistence/mapper/CoreMapper.java:68-74`
- **Code**: `toDomain()` has no mapping for entries (auto-maps), `toEntity()` ignores entries
- **Effect**: Every read of a MedicalRecord loads its entire clinical history, slow for patients with long treatment history

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |

## Scope

**In scope**:
- Add `@Mapping(target="entries", ignore=true)` to the `toDomain()` method for MedicalRecord

**Out of scope**:
- Do NOT change the domain MedicalRecord model
- Do NOT add lazy loading hints or fetch strategies (just ignore the collection)

## Git workflow

- Branch: `perf/012-medical-record-entries-mapping`
- Commit message: `perf: fix asymmetric CoreMapper medical record entries mapping to prevent lazy-load`

## Steps

### Step 1: Update CoreMapper MedicalRecord toDomain mapping

Edit `infrastructure/persistence/mapper/CoreMapper.java`:

Find the MedicalRecord mapping (around lines 68-74). Add `@Mapping` for entries:

```java
@Mapping(target = "patientId", source = "patient.id")
@Mapping(target = "entries", ignore = true)  // <-- ADD THIS LINE
MedicalRecord toDomain(MedicalRecordEntity entity);
```

**Verify**: `@Mapping(target="entries", ignore=true)` is now on the toDomain() method

### Step 2: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

- Existing tests should pass (entries were not being used in most tests anyway)
- If a test explicitly checks for entries in the domain object, that test may need updating

Verification: `./mvnw clean test` all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `CoreMapper.toDomain(MedicalRecordEntity)` has `@Mapping(target="entries", ignore=true)`
- [ ] Both toDomain() and toEntity() now ignore entries (symmetric)
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass

## STOP conditions

Stop and report if:

- A test fails because it expects entries to be populated in the domain object
- The MedicalRecord domain model has a different field name for entries (check the source)

## Maintenance notes

**Future changes**: If clinical history needs to be loaded in the domain, add a separate read path (e.g. `GetMedicalRecordWithEntriesUseCase`) instead of unconditionally loading entries on every read.

**Performance impact**: Modest (depends on entry count per patient), but compounds across many audit log views or report generation.
