# Plan DIRECTION-001: Implement use case for patients to join the waitlist

> **Executor instructions**: Read plan fully. This is a direction (feature) item, not a bug fix. It addresses a gap in the product. Do NOT push or commit.

## Status

- **Priority**: P2 (feature gap)
- **Effort**: M
- **Risk**: MEDIUM (new flow, needs integration with existing waitlist state machine)
- **Depends on**: none
- **Category**: direction/feature
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

**Evidence**: 
- Grep for `new WaitlistEntry(` returns zero results in src/main/java
- Only use cases exist for responding to offers (accept/decline), not joining
- `docs/testes/teste-fluxo-outros-fluxos.md` documents manual SQL inserts needed to test the waitlist
- `openspec/specs/waitlist/spec.md` describes the full flow (WAITING → OFFERED → ACCEPTED/DECLINED/EXPIRED), but no entry point exists

Currently, patients can't join the waitlist through the app. Admin/staff must insert directly via SQL. The feature is 70% complete (offer/accept/decline logic works) but has no entry point.

## Current state

- **Missing**: No endpoint, no use case, no controller method to let patient join waitlist
- **Existing**: `WaitlistEntry` domain model, repository, state machine (WAITING → OFFERED → ...), scheduler, public endpoints for response
- **Effect**: Waitlist feature unusable by end users; incomplete product

## Scope

**In scope**:
- Create `CreateWaitlistEntryUseCase` (request has: patientId, serviceId, datePreference)
- Add POST endpoint to `WaitlistPublicController` or new `WaitlistPatientController`
- Create request/response DTOs
- Add integration test

**Out of scope**:
- Do NOT change the state machine or offer/decline/accept logic
- Do NOT add capacity/slot management (that's a separate feature)
- Do NOT add notifications on join (use existing NotificationService, but don't enhance it here)

## Steps

### Step 1: Create CreateWaitlistEntryUseCase

Business logic:
- Accept patientId, serviceId, preferred date (optional)
- Verify patient and service exist
- Create WaitlistEntry in WAITING state
- Save to repository
- Return created entry with ID

**Verify**: Use case created, compiles

### Step 2: Create request/response DTOs

- `CreateWaitlistEntryRequest`: patientId, serviceId, preferredDateFrom?, preferredDateTo?
- `WaitlistEntryResponseDTO`: id, status, serviceId, createdAt, positionInQueue

**Verify**: DTOs created

### Step 3: Add endpoint to a controller

Either update `WaitlistPublicController` or create `WaitlistPatientController`:

```java
@PostMapping("/join")
public ResponseEntity<WaitlistEntryResponseDTO> joinWaitlist(
    @RequestBody CreateWaitlistEntryRequest request
) {
    WaitlistEntry created = createWaitlistEntryUseCase.execute(
        request.patientId(),
        request.serviceId(),
        request.preferredDateFrom(),
        request.preferredDateTo()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTO(created));
}
```

**Verify**: Endpoint accessible, returns 201

### Step 4: Add integration test

Test that:
- Patient can join waitlist
- Entry is created in WAITING state
- Returns 201 with entry ID
- Can't join twice for same service (optional: deduplicate or allow multiple)

**Verify**: Test passes

## Test plan

- Integration test: full flow from join → offer → accept
- Manual test: POST /api/v1/waitlist/join → verify entry created

## Done criteria

- [ ] `CreateWaitlistEntryUseCase` created
- [ ] DTOs created
- [ ] Endpoint implemented
- [ ] Integration test added
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] Endpoint returns 201 Created with entry ID

## STOP conditions

Stop and report if:

- Patient deduplication logic is needed (feature scope creep)
- Capacity/slot checking is required (separate feature)

## Maintenance notes

**Next steps**: After this is implemented, add:
- Patient-facing UI to join waitlist
- Notification when offer is made
- Patient dashboard to view waitlist status

**Integration**: The existing scheduler will automatically offer slots to waiting patients.
