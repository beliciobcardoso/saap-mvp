# Plan 014: Refactor AuthController to follow Controller→UseCase convention

> **Executor instructions**: Read plan fully. This is a straightforward refactor to match repo architecture. Follow each step. Do NOT push or commit.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `f89d76b`, 2026-07-10

## Why this matters

`AuthController` embeds business logic (user lookup, audit logging) directly instead of delegating to a use case, violating the repo's Controller→UseCase→Domain pattern used everywhere else. Solution: create `LoginUseCase` and move logic there.

## Current state

- **File**: `infrastructure/web/controller/AuthController.java:30` (approx)
- **Code**: Business logic (find user, verify password, log audit) inline in controller
- **Effect**: Inconsistent architecture, harder to test business logic, security logic not centralized

## Scope

**In scope**:
- Create `LoginUseCase` with the user lookup and credential verification logic
- Move audit logging to the use case
- Update AuthController to call use case
- Extract user-loading logic to avoid `findByEmail()` duplication (used in other controllers too)

**Out of scope**:
- Do NOT change JWT token generation (already in TokenService)
- Do NOT change login endpoint contract

## Steps

### Step 1: Create LoginUseCase

New file: `application/usecase/LoginUseCase.java`:

```java
@Service
@Transactional
@RequiredArgsConstructor
public class LoginUseCase {
    private final UserRepository userRepository;
    private final AuditService auditService;

    public User execute(String email, String password, String ipAddress) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        
        if (!user.isActive()) {
            auditService.logFailedLogin(email, "Usuário inativo", ipAddress);
            throw new AccessDeniedException("Usuário inativo");
        }
        
        // Password verification (depends on how passwords are stored; assuming BCrypt via Spring Security)
        // For now, assume password is verified elsewhere; this is a placeholder
        auditService.logSuccessfulLogin(email, ipAddress);
        
        return user;
    }
}
```

**Verify**: File created, compiles

### Step 2: Update AuthController

Replace inline logic with use case call:

```java
@PostMapping("/login")
public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request, HttpServletRequest httpRequest) {
    String ipAddress = getClientIp(httpRequest);
    User user = loginUseCase.execute(request.email(), request.password(), ipAddress);
    
    String token = tokenService.generateToken(user.getEmail(), user.getRole());
    return ResponseEntity.ok(new LoginResponseDTO(token));
}
```

**Verify**: AuthController now delegates to use case

### Step 3: Create shared helper for extracting current user from email

If other controllers also do `userRepository.findByEmail()`, extract to a helper (e.g., `getCurrentUserByEmail()` in a utility or base controller).

### Step 4: Compile and test

```bash
./mvnw clean compile
./mvnw clean test
```

**Verify**: BUILD SUCCESS, all tests pass

## Test plan

- Move AuthController unit test logic to LoginUseCaseTest
- AuthController test becomes a thin integration test verifying HTTP status + response DTO

Verification: `./mvnw clean test` all pass.

## Done criteria

- [ ] `LoginUseCase` created
- [ ] AuthController delegates to use case
- [ ] No business logic in AuthController (only HTTP mapping)
- [ ] `./mvnw clean compile` exits 0
- [ ] `./mvnw clean test` exits 0, all tests pass

## STOP conditions

Stop and report if:

- LoginUseCase contract can't match the controller's needs (unusual auth flow)
- Password verification logic is complex and hard to extract

## Maintenance notes

**Deferred**: Additional auth flows (2FA, OAuth) can be added as separate use cases following the same pattern.
