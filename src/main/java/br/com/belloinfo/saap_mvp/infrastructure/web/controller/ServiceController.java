package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.service.AuditService;
import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.PageResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.ServiceRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.ServiceResponseDTO;
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
@RequestMapping("/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ServiceController {

    private final CreateServiceUseCase createServiceUseCase;
    private final FindServiceByIdUseCase findServiceByIdUseCase;
    private final ListActiveServicesUseCase listActiveServicesUseCase;
    private final UpdateServiceUseCase updateServiceUseCase;
    private final DeactivateServiceUseCase deactivateServiceUseCase;
    private final AuditService auditService;
    private final WebMapper mapper;

    private String getAuthenticatedUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymous@saap.com";
    }

    @PostMapping
    public ResponseEntity<ServiceResponseDTO> create(@Valid @RequestBody ServiceRequestDTO request, HttpServletRequest httpRequest) {
        Service service = mapper.toDomain(request);
        Service saved = createServiceUseCase.execute(service);

        String email = getAuthenticatedUserEmail();
        auditService.log("CADASTRO_SERVICO", saved.getId(), "SERVICE", email, httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ServiceResponseDTO> findById(@PathVariable UUID id) {
        return findServiceByIdUseCase.execute(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<PageResponseDTO<ServiceResponseDTO>> listAllActive(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PageResponseDTO.from(listActiveServicesUseCase.execute(page, size), mapper::toResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ServiceRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        Service service = mapper.toDomain(request);
        Service updated = updateServiceUseCase.execute(id, service);

        String email = getAuthenticatedUserEmail();
        auditService.log("ATUALIZACAO_SERVICO", updated.getId(), "SERVICE", email, httpRequest.getRemoteAddr());

        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id, HttpServletRequest httpRequest) {
        deactivateServiceUseCase.execute(id);

        String email = getAuthenticatedUserEmail();
        auditService.log("DESATIVACAO_SERVICO", id, "SERVICE", email, httpRequest.getRemoteAddr());

        return ResponseEntity.noContent().build();
    }
}
