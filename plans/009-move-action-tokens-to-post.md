# Plan 009: Move action tokens from GET query string to POST body

> **Executor instructions**: Read the plan fully, then follow each step. Run verification commands and confirm expected results before moving to next step. If you hit a STOP condition, stop and report the exact error. Do NOT push or commit — main agent handles git.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MEDIUM (changes public endpoint contracts, backward compatibility concern)
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

Action tokens (confirm/cancel appointment, accept/decline waitlist offer) are sent via GET query string: `/api/v1/appointments/public/confirm?token=xyz`. GET requests are logged by browsers, proxies, WAF, CDN — token leakage vectors. Solution: POST to a form or JSON body instead, where tokens are NOT logged.

## Current state

- **Files**: `infrastructure/web/controller/AppointmentController.java`, `infrastructure/web/controller/WaitlistPublicController.java`
- **Endpoints**: 
  - `GET /api/v1/appointments/public/confirm?token=...`
  - `GET /api/v1/appointments/public/cancel?token=...`
  - `GET /api/v1/waitlist/public/accept?token=...`
  - `GET /api/v1/waitlist/public/decline?token=...`
- **Effect**: Tokens in query strings are logged in HTTP referer headers, access logs, browser history

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |

## Scope

**In scope**:
- Convert 4 GET endpoints to POST
- Create `@RequestBody` DTOs for token payload
- Update test .http files (if present)
- Update integration tests to use POST instead of GET

**Out of scope**:
- Do NOT change token validation logic
- Do NOT change response shapes
- Do NOT add new token types or features

## Git workflow

- Branch: `fix/009-action-tokens-post`
- Commit message: `fix: move action tokens from GET query string to POST body (prevent logging leakage)`

## Steps

### Step 1: Create request DTOs for token payloads

Create new file: `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/dto/ActionTokenRequest.java`

```java
package br.com.belloinfo.saap_mvp.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ActionTokenRequest(
    @NotBlank(message = "Token é obrigatório")
    String token
) {}
```

**Verify**: File exists, record syntax valid

### Step 2: Update AppointmentController endpoints

In `AppointmentController.java`, find the two endpoints:
- `@GetMapping("/public/confirm")`
- `@GetMapping("/public/cancel")`

Replace each with `@PostMapping` and add `@RequestBody ActionTokenRequest request`:

```java
@PostMapping("/public/confirm")
public ResponseEntity<AppointmentResponseDTO> confirmAppointmentByToken(
    @RequestBody ActionTokenRequest request
) {
    return ResponseEntity.ok(confirmAppointmentByTokenUseCase.execute(request.token()));
}

@PostMapping("/public/cancel")
public ResponseEntity<AppointmentResponseDTO> cancelAppointmentByToken(
    @RequestBody ActionTokenRequest request
) {
    return ResponseEntity.ok(cancelAppointmentByTokenUseCase.execute(request.token()));
}
```

**Verify**: Methods now @PostMapping with @RequestBody

### Step 3: Update WaitlistPublicController endpoints

Same pattern for:
- `@GetMapping("/public/accept")`
- `@GetMapping("/public/decline")`

Replace with @PostMapping + @RequestBody ActionTokenRequest

**Verify**: Methods updated to POST

### Step 4: Update integration tests

Find tests that call these endpoints (likely in `*PublicControllerIntegrationTest.java`).

Change from:
```java
restTemplate.getForObject("/api/v1/appointments/public/confirm?token={token}", ...)
```

To:
```java
restTemplate.postForObject(
    "/api/v1/appointments/public/confirm",
    new ActionTokenRequest(token),
    AppointmentResponseDTO.class
)
```

**Verify**: Tests updated and passing

### Step 5: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

- Update existing integration tests to use POST instead of GET
- Verify token validation still works (same logic, just different HTTP method)
- Verify response shapes unchanged
- New test: confirm tokens in POST body are NOT accessible via GET anymore

Verification: `./mvnw clean test` all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `ActionTokenRequest` record created
- [ ] AppointmentController: 2 endpoints changed from @GetMapping to @PostMapping
- [ ] WaitlistPublicController: 2 endpoints changed from @GetMapping to @PostMapping
- [ ] All 4 endpoints accept @RequestBody ActionTokenRequest
- [ ] Integration tests updated to POST instead of GET
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass
- [ ] No GET endpoints for public token actions (grep returns zero /public/confirm or /public/cancel using GET)

## STOP conditions

Stop and report if:

- A GET request to the old endpoint is still used by integration tests after updating
- The use case expects token as a different parameter name
- Response DTOs changed unexpectedly

## Maintenance notes

**Future changes**: If frontend is consuming these endpoints, update client code to POST with JSON body instead of query string.

**Deferred**: Rate limiting on POST to prevent token brute-force is separate (plan for future).
