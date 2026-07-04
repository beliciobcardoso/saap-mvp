package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.service.AuditService;
import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.PageResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.UserRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.UserResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final FindUserByIdUseCase findUserByIdUseCase;
    private final ListActiveUsersUseCase listActiveUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeactivateUserUseCase deactivateUserUseCase;
    private final AuditService auditService;
    private final WebMapper mapper;

    private String getAuthenticatedUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymous@saap.com";
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO request, HttpServletRequest httpRequest) {
        User user = mapper.toDomain(request);
        User saved = createUserUseCase.execute(user);

        String email = getAuthenticatedUserEmail();
        auditService.log("CADASTRO_USUARIO", saved.getId(), "USER", email, httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable UUID id) {
        return findUserByIdUseCase.execute(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<PageResponseDTO<UserResponseDTO>> listAllActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponseDTO.from(listActiveUsersUseCase.execute(page, size), mapper::toResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        User user = mapper.toDomain(request);
        User updated = updateUserUseCase.execute(id, user);

        String email = getAuthenticatedUserEmail();
        auditService.log("ATUALIZACAO_USUARIO", updated.getId(), "USER", email, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, HttpServletRequest httpRequest) {
        deactivateUserUseCase.execute(id);

        String email = getAuthenticatedUserEmail();
        auditService.log("DESATIVACAO_USUARIO", id, "USER", email, httpRequest.getRemoteAddr());

        return ResponseEntity.noContent().build();
    }
}
