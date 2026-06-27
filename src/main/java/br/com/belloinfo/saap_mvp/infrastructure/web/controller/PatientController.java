package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Patient;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.PatientRequestDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.PatientResponseDTO;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final CreatePatientUseCase createPatientUseCase;
    private final FindPatientByIdUseCase findPatientByIdUseCase;
    private final ListActivePatientsUseCase listActivePatientsUseCase;
    private final UpdatePatientUseCase updatePatientUseCase;
    private final DeactivatePatientUseCase deactivatePatientUseCase;
    private final WebMapper mapper;

    @PostMapping
    public ResponseEntity<PatientResponseDTO> create(@Valid @RequestBody PatientRequestDTO request) {
        Patient patient = mapper.toDomain(request);
        Patient saved = createPatientUseCase.execute(patient);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> findById(@PathVariable UUID id) {
        return findPatientByIdUseCase.execute(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> listAllActive() {
        List<PatientResponseDTO> responses = listActivePatientsUseCase.execute().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody PatientRequestDTO request) {
        Patient patient = mapper.toDomain(request);
        Patient updated = updatePatientUseCase.execute(id, patient);
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivatePatientUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
