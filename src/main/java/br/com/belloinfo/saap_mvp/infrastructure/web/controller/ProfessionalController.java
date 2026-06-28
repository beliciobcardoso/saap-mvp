package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.service.AuditService;
import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.ProfessionalRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.ProfessionalResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProfessionalController {

    private final CreateProfessionalUseCase createProfessionalUseCase;
    private final FindProfessionalByIdUseCase findProfessionalByIdUseCase;
    private final ListActiveProfessionalsUseCase listActiveProfessionalsUseCase;
    private final UpdateProfessionalUseCase updateProfessionalUseCase;
    private final DeactivateProfessionalUseCase deactivateProfessionalUseCase;
    private final AuditService auditService;
    private final WebMapper mapper;

    private String getAuthenticatedUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymous@saap.com";
    }

    @PostMapping
    public ResponseEntity<ProfessionalResponseDTO> create(@Valid @RequestBody ProfessionalRequestDTO request, HttpServletRequest httpRequest) {
        Professional professional = mapper.toDomain(request);
        Professional saved = createProfessionalUseCase.execute(professional);

        String email = getAuthenticatedUserEmail();
        auditService.log("CADASTRO_PROFISSIONAL", saved.getId(), "PROFESSIONAL", email, httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ProfessionalResponseDTO> findById(@PathVariable UUID id) {
        return findProfessionalByIdUseCase.execute(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<List<ProfessionalResponseDTO>> listAllActive() {
        List<ProfessionalResponseDTO> responses = listActiveProfessionalsUseCase.execute().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProfessionalRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        Professional professional = mapper.toDomain(request);
        Professional updated = updateProfessionalUseCase.execute(id, professional);

        String email = getAuthenticatedUserEmail();
        auditService.log("ATUALIZACAO_PROFISSIONAL", updated.getId(), "PROFESSIONAL", email, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, HttpServletRequest httpRequest) {
        deactivateProfessionalUseCase.execute(id);

        String email = getAuthenticatedUserEmail();
        auditService.log("DESATIVACAO_PROFISSIONAL", id, "PROFESSIONAL", email, httpRequest.getRemoteAddr());

        return ResponseEntity.noContent().build();
    }
}
