# Plan DIRECTION-003: Implement PATIENT user role for authenticated patient access (currently unused)

> **Executor instructions**: Read plan fully. This is a direction (feature) item. Do NOT push or commit.

## Status

- **Priority**: P2 (feature gap)
- **Effort**: M
- **Risk**: MEDIUM (adds new access path, needs authorization gates)
- **Depends on**: none
- **Category**: direction/feature
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

**Evidence**:
- `UserRole.PATIENT` exists in enum (`domain/valueobject/UserRole.java`)
- Documented in RBAC section of CLAUDE.md as a role type
- Zero `@PreAuthorize("hasRole('PATIENT')")` in codebase
- No endpoint, no use case, no controller accepts PATIENT role
- Zero flow creates a PATIENT user (only ADMIN, RECEPTIONIST, PROFESSIONAL, ASSISTANT)

Currently, patients interact with the app via stateless action tokens (confirm/cancel appointment, accept/decline waitlist offer) without login. `UserRole.PATIENT` is defined but never used, creating dead code and confusion.

**Product gap**: Patients can't log in, view their appointments, or manage their profile. All patient-facing features are hidden behind action tokens.

## Current state

- **Defined**: `UserRole.PATIENT` in enum
- **Not used**: Zero authorization gates, endpoints, or flows use this role
- **Missing**: Patient login, patient dashboard, patient API endpoints
- **Effect**: Dead code; PATIENT role unusable; patients can't self-serve

## Scope

**In scope**:
- Create `CreatePatientUserUseCase` (register/create patient with PATIENT role)
- Add `@PreAuthorize("hasRole('PATIENT')")` gates to new patient endpoints
- Create patient-facing endpoints:
  - GET `/api/v1/patients/me` (current patient's profile)
  - GET `/api/v1/patients/me/appointments` (my appointments)
  - PUT `/api/v1/patients/me` (update my profile)
  - GET `/api/v1/patients/me/waitlist-entries` (my waitlist entries)
- Add integration test for patient login and access

**Out of scope**:
- Do NOT change the existing action-token endpoints (those remain stateless)
- Do NOT add patient UI/frontend (separate project)
- Do NOT add appointment creation for patients (must go through waitlist or receptionist)

## Steps

### Step 1: Create CreatePatientUserUseCase

Business logic:
- Accept: cpf, email, name, password
- Verify CPF not already in use
- Hash password with Spring Security's PasswordEncoder
- Create User with role=PATIENT
- Save to repository
- Return created user (without password)

**Verify**: Use case created

### Step 2: Add patient registration endpoint

Controller:
```java
@PostMapping("/register")
public ResponseEntity<UserResponseDTO> registerPatient(
    @RequestBody CreatePatientUserRequest request
) {
    User patient = createPatientUserUseCase.execute(
        request.cpf(),
        request.email(),
        request.name(),
        request.password()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTO(patient));
}
```

No `@PreAuthorize` (public registration).

**Verify**: Endpoint accessible

### Step 3: Add patient-only endpoints

Controller with `@PreAuthorize("hasRole('PATIENT')")`:

```java
@GetMapping("/me")
@PreAuthorize("hasRole('PATIENT')")
public ResponseEntity<PatientProfileDTO> getMyProfile() {
    Patient patient = getCurrentPatient();
    return ResponseEntity.ok(mapper.toDTO(patient));
}

@GetMapping("/me/appointments")
@PreAuthorize("hasRole('PATIENT')")
public ResponseEntity<List<AppointmentResponseDTO>> getMyAppointments() {
    Patient patient = getCurrentPatient();
    List<Appointment> appointments = appointmentRepository.findByPatientId(patient.getId());
    return ResponseEntity.ok(appointments.stream().map(mapper::toDTO).collect(toList()));
}
```

**Verify**: Endpoints require PATIENT role

### Step 4: Add integration test

Test that:
- Patient can register (201)
- Patient can log in and get JWT token with ROLE_PATIENT
- Patient can access `/me` endpoint (200)
- Non-patient cannot access `/me` endpoint (403)
- Patient sees only their own appointments

**Verify**: Test passes

## Test plan

- Registration flow: create patient, verify user in DB with PATIENT role
- Authentication: login as patient, verify JWT includes ROLE_PATIENT
- Authorization: try accessing patient endpoints as different role (should 403)
- Data isolation: verify patient sees only their data

## Done criteria

- [ ] `CreatePatientUserUseCase` created
- [ ] Patient registration endpoint implemented (public, no auth required)
- [ ] Patient profile endpoint implemented with `@PreAuthorize("hasRole('PATIENT'")`
- [ ] My appointments endpoint implemented with PATIENT gate
- [ ] Integration test covers registration → login → access patient endpoints
- [ ] `./mvnw clean test` exits 0

## STOP conditions

Stop and report if:

- Password hashing is not configured (should be auto via Spring Security)
- Patient entity/model doesn't exist (should reuse User model with PATIENT role)

## Maintenance notes

**Integration with action tokens**: Existing action-token flows remain unchanged. Patients can:
- Use action tokens for quick confirm/cancel without login, OR
- Log in as PATIENT and manage via dashboard

**Future**: Add password reset, 2FA, profile picture, notification preferences via authenticated endpoints.

**Note**: UserRole.PATIENT becomes "implemented but optional" — patients can still use action tokens, but can also self-serve via login if they prefer.
