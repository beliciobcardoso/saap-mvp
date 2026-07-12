package br.com.belloinfo.saap_mvp.infrastructure.web.controller;

import br.com.belloinfo.saap_mvp.application.usecase.*;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.User;
import br.com.belloinfo.saap_mvp.domain.model.Professional;
import br.com.belloinfo.saap_mvp.domain.repository.UserRepository;
import br.com.belloinfo.saap_mvp.domain.repository.ProfessionalRepository;
import br.com.belloinfo.saap_mvp.infrastructure.security.SecurityUtils;
import br.com.belloinfo.saap_mvp.infrastructure.web.dto.*;
import br.com.belloinfo.saap_mvp.infrastructure.web.mapper.WebMapper;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private final FindAppointmentByIdUseCase findAppointmentByIdUseCase;
    private final ConfirmAppointmentByTokenUseCase confirmAppointmentByTokenUseCase;
    private final CancelAppointmentByTokenUseCase cancelAppointmentByTokenUseCase;
    private final AcceptWaitlistOfferUseCase acceptWaitlistOfferUseCase;
    private final DeclineWaitlistOfferUseCase declineWaitlistOfferUseCase;
    private final CallNextPatientUseCase callNextPatientUseCase;
    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final br.com.belloinfo.saap_mvp.application.service.AuditService auditService;
    private final WebMapper mapper;

    private void logAudit(String action, UUID resourceId, HttpServletRequest httpRequest) {
        String userEmail = SecurityUtils.getAuthenticatedUserEmail();
        if (!"anonymous@saap.com".equals(userEmail)) {
            auditService.log(action, resourceId, "APPOINTMENT", userEmail, httpRequest.getRemoteAddr());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<AppointmentResponseDTO> book(@Valid @RequestBody BookAppointmentRequestDTO request, HttpServletRequest httpRequest) {
        Appointment appointment = bookAppointmentUseCase.execute(
                request.patientId(),
                request.professionalId(),
                request.serviceId(),
                request.dateTime(),
                request.paymentMethod(),
                request.declaredPriority()
        );
        logAudit("CADASTRO_AGENDAMENTO", appointment.getId(), httpRequest);
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
    public ResponseEntity<PageResponseDTO<AppointmentResponseDTO>> list(
            @RequestParam(required = false) UUID professionalId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(PageResponseDTO.from(
                listAppointmentsUseCase.execute(professionalId, patientId, start, end, page, size), mapper::toResponse));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentResponseDTO> confirm(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Appointment appointment = confirmAppointmentUseCase.execute(id);
        logAudit("CONFIRMACAO_AGENDAMENTO", appointment.getId(), httpRequest);
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'PATIENT')")
    public ResponseEntity<AppointmentResponseDTO> cancel(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Appointment appointment = cancelAppointmentUseCase.execute(id);
        logAudit("CANCELAMENTO_AGENDAMENTO", appointment.getId(), httpRequest);
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<AppointmentResponseDTO> checkIn(
            @PathVariable UUID id,
            @Valid @RequestBody CheckInRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        UUID verifiedById = request.verifiedBy();
        if (verifiedById == null) {
            String userEmail = SecurityUtils.getAuthenticatedUserEmail();
            if (!"anonymous@saap.com".equals(userEmail)) {
                User user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new IllegalArgumentException("Usuário logado não encontrado"));
                verifiedById = user.getId();
            }
        }

        Appointment appointment = checkInAppointmentUseCase.execute(
                id,
                request.verifiedLevel(),
                verifiedById,
                request.notes(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PostMapping("/next")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<AppointmentResponseDTO> callNext(
            HttpServletRequest httpRequest
    ) {
        String email = SecurityUtils.getAuthenticatedUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário logado não encontrado"));
        Professional professional = professionalRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profissional não cadastrado para o usuário logado"));

        Appointment appointment = callNextPatientUseCase.execute(
                professional.getId(),
                user.getId(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
    public ResponseEntity<AppointmentResponseDTO> start(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Appointment appointment = startAppointmentUseCase.execute(id);
        logAudit("INICIO_ATENDIMENTO", appointment.getId(), httpRequest);
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
    public ResponseEntity<AppointmentResponseDTO> complete(@PathVariable UUID id, HttpServletRequest httpRequest) {
        Appointment appointment = completeAppointmentUseCase.execute(id);
        logAudit("FINALIZACAO_ATENDIMENTO", appointment.getId(), httpRequest);
        return ResponseEntity.ok(mapper.toResponse(appointment));
    }

    @GetMapping("/public/confirm")
    public ResponseEntity<String> publicConfirm(@RequestParam String token) {
        try {
            confirmAppointmentByTokenUseCase.execute(token);
            return ResponseEntity.ok("Presença confirmada com sucesso!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/public/cancel")
    public ResponseEntity<String> publicCancel(@RequestParam String token) {
        try {
            cancelAppointmentByTokenUseCase.execute(token);
            return ResponseEntity.ok("Consulta cancelada com sucesso!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/public/waitlist/accept")
    public ResponseEntity<String> publicWaitlistAccept(@RequestParam String token) {
        try {
            acceptWaitlistOfferUseCase.execute(token);
            return ResponseEntity.ok("Vaga da fila de espera aceita e agendamento confirmado com sucesso!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/public/waitlist/decline")
    public ResponseEntity<String> publicWaitlistDecline(@RequestParam String token) {
        try {
            declineWaitlistOfferUseCase.execute(token);
            return ResponseEntity.ok("Vaga da fila de espera recusada com sucesso.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
