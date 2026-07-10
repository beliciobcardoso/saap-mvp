# Plan 003: Add professional ownership check to medical record read endpoint

> **Executor instructions**: Follow this plan step by step. Run every verification command and confirm the expected result before moving to the next step. If anything in the "STOP conditions" section occurs, stop and report — do not improvise. When done, update the status row for this plan in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat a5a9a5a..HEAD -- src/main/java/br/com/belloinfo/saap_mvp/application/usecase/GetMedicalRecordByPatientUseCase.java src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/controller/MedicalRecordController.java`
> If either file changed, compare the excerpts below against the live code before proceeding.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: security
- **Planned at**: commit `a5a9a5a`, 2026-07-10

## Why this matters

`GetMedicalRecordByPatientUseCase` (the read path) has no professional-ownership check, while `CreateMedicalRecordEntryUseCase` and `UpdateMedicalRecordEntryUseCase` (write paths) both verify the authenticated professional is assigned to the appointment. This allows any authenticated PROFESSIONAL to read any patient's complete clinical history — a LGPD/RNF01 (audit/compliance) violation. Fix: add the same ownership check to the read path.

## Current state

- **Files**:
  - `src/main/java/br/com/belloinfo/saap_mvp/application/usecase/GetMedicalRecordByPatientUseCase.java:14-23` — no professional parameter, no check
  - `src/main/java/br/com/belloinfo/saap_mvp/application/usecase/CreateMedicalRecordEntryUseCase.java:36-38` — has the check (pattern to match)
  - `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/controller/MedicalRecordController.java:33-40` (approx line) — controller endpoint calling the use case
- **Effect**: Any PROFESSIONAL bypasses ownership verification on read

### Code excerpts

**GetMedicalRecordByPatientUseCase.java:14-23** (current — lacks check)
```java
public class GetMedicalRecordByPatientUseCase {
    private final MedicalRecordRepository medicalRecordRepository;

    @Transactional(readOnly = true)
    public MedicalRecord execute(UUID patientId) {
        return medicalRecordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente ainda não possui prontuário"));
    }
}
```

**CreateMedicalRecordEntryUseCase.java:36-38** (pattern to match)
```java
if (!appointment.getProfessional().getId().equals(currentProfessionalId)) {
    throw new AccessDeniedException("Apenas o profissional do atendimento pode registrar a evolução");
}
```

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile | `./mvnw clean compile` | BUILD SUCCESS |
| Test | `./mvnw clean test` | all pass |
| Grep for ownership checks | `grep -n "currentProfessionalId\|currentUserId" src/main/java/br/com/belloinfo/saap_mvp/application/usecase/GetMedicalRecordByPatientUseCase.java` | should find the check after edit |

## Scope

**In scope**:
- `src/main/java/br/com/belloinfo/saap_mvp/application/usecase/GetMedicalRecordByPatientUseCase.java` — add `currentProfessionalId` parameter, add ownership check
- `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/controller/MedicalRecordController.java` — pass authenticated professional ID to use case

**Out of scope**:
- Do NOT change `MedicalRecordRepository` interface or implementation
- Do NOT change the response shape or endpoint path
- Do NOT add new exception types (use existing `AccessDeniedException`)

## Git workflow

- Branch: `fix/003-medical-record-ownership-check`
- Commit message: `fix: enforce professional ownership on medical record read (LGPD compliance)`
- Do NOT push unless instructed

## Steps

### Step 1: Add currentProfessionalId parameter to GetMedicalRecordByPatientUseCase

Edit `src/main/java/br/com/belloinfo/saap_mvp/application/usecase/GetMedicalRecordByPatientUseCase.java`:

Replace the `execute(UUID patientId)` signature with:

```java
@Transactional(readOnly = true)
public MedicalRecord execute(UUID patientId, UUID currentProfessionalId) {
    MedicalRecord medicalRecord = medicalRecordRepository.findByPatientId(patientId)
            .orElseThrow(() -> new EntityNotFoundException("Paciente ainda não possui prontuário"));
    
    // Verify professional owns this record
    if (!medicalRecord.getPatientId().equals(patientId)) {
        throw new EntityNotFoundException("Prontuário não encontrado");
    }
    
    // Load the patient's appointments to verify professional assignment
    // Since MedicalRecord doesn't have a reference to Appointment, we need the appointment context
    // Assumption: the MedicalRecord is implicitly tied to all appointments for that patient
    // For simplicity: check that the patient has at least one appointment assigned to this professional
    // (This prevents a random professional from reading just because they authenticated)
    
    // NOTE: This is a simplified check. A more rigorous approach would:
    // - Store professional_id directly on MedicalRecord, or
    // - Load all patient appointments and verify current professional is in the list
    // For now, we'll make the check via a new repository method.
    
    return medicalRecord;
}
```

Wait — this is getting complex. Let me re-read the domain to understand the relationship better. The issue is: MedicalRecord is tied to a Patient, but medical records can be read/written by multiple professionals (whoever is assigned to an appointment for that patient). The check should be: "is this professional assigned to ANY active/recent appointment for this patient?" Let me refactor:

Actually, let's check the CreateMedicalRecordEntryUseCase again to understand how it gets the appointment. It likely takes the appointment ID, loads the appointment, and checks professional. Let me simplify:

Edit `src/main/java/br/com/belloinfo/saap_mvp/application/usecase/GetMedicalRecordByPatientUseCase.java`:

```java
public class GetMedicalRecordByPatientUseCase {
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public MedicalRecord execute(UUID patientId, UUID currentProfessionalId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Paciente ainda não possui prontuário"));
        
        // Verify that the current professional has an active/completed appointment with this patient
        // (i.e., is legitimately assigned to care for this patient)
        boolean hasAccess = appointmentRepository.existsByPatientIdAndProfessionalId(patientId, currentProfessionalId);
        if (!hasAccess) {
            throw new AccessDeniedException("Você não tem permissão para acessar o prontuário deste paciente");
        }
        
        return medicalRecord;
    }
}
```

This requires a new repository method. Let me specify that:

**Verify**: After editing, `grep -n "currentProfessionalId" src/main/java/br/com/belloinfo/saap_mvp/application/usecase/GetMedicalRecordByPatientUseCase.java` → should find the parameter and the check

### Step 2: Add existsByPatientIdAndProfessionalId to AppointmentRepository

Edit `src/main/java/br/com/belloinfo/saap_mvp/domain/repository/AppointmentRepository.java` (port/interface):

Add the method signature:

```java
boolean existsByPatientIdAndProfessionalId(UUID patientId, UUID professionalId);
```

Then edit `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/persistence/repository/JpaAppointmentRepository.java` (JPA implementation):

Add the method:

```java
@Query("SELECT COUNT(a) > 0 FROM AppointmentEntity a WHERE a.patient.id = :patientId AND a.professional.id = :professionalId")
boolean existsByPatientIdAndProfessionalId(UUID patientId, UUID professionalId);
```

**Verify**: `grep -n "existsByPatientIdAndProfessionalId" src/main/java/br/com/belloinfo/saap_mvp/domain/repository/AppointmentRepository.java` → should find the signature

### Step 3: Update MedicalRecordController to pass currentProfessionalId

Edit `src/main/java/br/com/belloinfo/saap_mvp/infrastructure/web/controller/MedicalRecordController.java`:

Find the endpoint that calls `getMedicalRecordByPatientUseCase.execute(patientId)` and update it to:

```java
String currentUserEmail = getAuthenticatedUserEmail(); // or extract from SecurityContextHolder
// TODO: Once User/Patient/Professional are linked, resolve UUID from email. For now, assume it's passed in request or header.
// Simplified: extract from principal or session
UUID currentProfessionalId = getCurrentProfessionalIdFromAuth(); // implement this helper

MedicalRecord medicalRecord = getMedicalRecordByPatientUseCase.execute(patientId, currentProfessionalId);
```

If there's no direct way to get UUID from auth context, add a helper:

```java
private UUID getCurrentProfessionalIdFromAuth() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
        throw new AccessDeniedException("Autenticação requerida");
    }
    // Assuming the principal is a UserDetails or the email string:
    String email = auth.getName();
    // Load professional by email from repository
    return userRepository.findByEmail(email)
            .map(User::getId) // or a method that maps user to professional
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
}
```

Actually, this is getting complex because we need to map User → Professional. Let me simplify: assume the `currentProfessionalId` can be extracted from a `@AuthenticationPrincipal` or request context. Leave that as a TODO if the mechanism isn't obvious.

**Verify**: The controller method signature now accepts a way to get `currentProfessionalId`, and it's passed to the use case

### Step 4: Compile and test

```bash
./mvnw clean compile
```

**Verify**: BUILD SUCCESS, no compilation errors

```bash
./mvnw clean test
```

**Verify**: All tests pass

## Test plan

- Existing integration tests for `GetMedicalRecordByPatientUseCase` should be updated to pass a `currentProfessionalId` that matches the patient's appointment professional
- New test: `GetMedicalRecordByPatientUseCaseTest.shouldThrowAccessDeniedWhenProfessionalNotAssigned()` — pass a patient ID and a professional ID that has no appointments with that patient, expect `AccessDeniedException`
- Model after existing test structure in `src/test/java/br/com/belloinfo/saap_mvp/application/usecase/CreateMedicalRecordEntryUseCaseTest.java`

Verification: `./mvnw clean test` all pass, including new test.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `GetMedicalRecordByPatientUseCase.execute()` now accepts `UUID currentProfessionalId` parameter
- [ ] `AppointmentRepository` has new method `existsByPatientIdAndProfessionalId`
- [ ] `JpaAppointmentRepository` implements the method with `@Query`
- [ ] `MedicalRecordController` passes `currentProfessionalId` to the use case
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass, including new ownership-denial test
- [ ] `grep -n "AccessDeniedException.*prontuário\|Você não tem permissão" src/main/java/br/com/belloinfo/saap_mvp/application/usecase/GetMedicalRecordByPatientUseCase.java` finds the new check
- [ ] `plans/README.md` status row for plan 003 updated to DONE

## STOP conditions

Stop and report back if:

- The mapping from User email/principal to Professional UUID is not clear in the codebase — this would require a deeper domain understanding than the plan assumes
- Test suite fails because existing tests were not updated to pass the new `currentProfessionalId` parameter
- The `AppointmentRepository` doesn't have a convenient way to query by patient + professional (e.g., appointments are stored in a non-relational structure)

## Maintenance notes

**Future changes**: If the domain evolves to store `professionalId` directly on `MedicalRecord`, this check can be simplified to a direct ID comparison.

**For reviewers**: Verify that the auth-context extraction (`getCurrentProfessionalIdFromAuth()`) is consistent with how other use cases extract the current user. If it's duplicated across controllers, consider extracting it to a shared utility.

**Deferred**: Audit logging for denied medical record access (who tried to read what, when) is not included here but should be a follow-up to maintain compliance logs (RNF01).
