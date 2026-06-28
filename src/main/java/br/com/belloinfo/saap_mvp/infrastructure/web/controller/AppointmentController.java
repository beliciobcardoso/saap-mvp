package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.*;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final BookAppointmentUseCase bookAppointmentUseCase;
    private final ConfirmAppointmentUseCase confirmAppointmentUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;
    private final CheckInAppointmentUseCase checkInAppointmentUseCase;
    private final StartAppointmentUseCase startAppointmentUseCase;
    private final CompleteAppointmentUseCase completeAppointmentUseCase;
    private final ListAppointmentsUseCase listAppointmentsUseCase;
    private final FindAppointmentByIdUseCase findAppointmentByIdUseCase; // Let's make sure we implement this or use Jpa directly. Wait, we should implement FindAppointmentByIdUseCase! Let's check.
    private final WebMapper mapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<AppointmentResponseDTO> book(@Valid @RequestBody BookAppointmentRequestDTO request) {
        Appointment appointment = bookAppointmentUseCase.execute(
                request.patientId(),
                request.professionalId(),
                request.serviceId(),
                request.dateTime(),
                request.paymentMethod(),
                request.declaredPriority()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(appointment));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PROFESSIONAL', 'PATIENT')")
    public ResponseEntity<AppointmentResponseDTO> findById(@PathVariable UUID id) {
        return findAppointmentByIdUseCase.execute(id)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PROFESSIONAL', 'PATIENT')")
    public ResponseEntity<List<AppointmentResponseDTO>> list(
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        List<AppointmentResponseDTO> responseList = listAppointmentsUseCase.execute(professionalId, patientId, start, end).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentResponseDTO> confirm(@PathVariable UUID id) {
        Appointment appointment = confirmAppointmentUseCase.execute(id);
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<AppointmentResponseDTO> cancel(@PathVariable UUID id) {
        Appointment appointment = cancelAppointmentUseCase.execute(id);
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentResponseDTO> checkIn(
            @PathVariable UUID id,
            @Valid @RequestBody CheckInRequestDTO request
    ) {
        Appointment appointment = checkInAppointmentUseCase.execute(
                id,
                request.verifiedLevel(),
                request.verifiedBy(),
                request.notes()
        );
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
    public ResponseEntity<AppointmentResponseDTO> start(@PathVariable UUID id) {
        Appointment appointment = startAppointmentUseCase.execute(id);
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
    public ResponseEntity<AppointmentResponseDTO> complete(@PathVariable UUID id) {
        Appointment appointment = completeAppointmentUseCase.execute(id);
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }
}
