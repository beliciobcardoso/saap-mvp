package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Service;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.ServiceRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.ServiceResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final WebMapper mapper;

    @PostMapping
    public ResponseEntity<ServiceResponseDTO> create(@Valid @RequestBody ServiceRequestDTO request) {
        Service service = mapper.toDomain(request);
        Service saved = createServiceUseCase.execute(service);
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
    public ResponseEntity<List<ServiceResponseDTO>> listAllActive() {
        List<ServiceResponseDTO> responses = listActiveServicesUseCase.execute().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody ServiceRequestDTO request) {
        Service service = mapper.toDomain(request);
        Service updated = updateServiceUseCase.execute(id, service);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivateServiceUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
