package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.AuditLog;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.AuditLogRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CallNextPatientUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public Appointment execute(UUID professionalId, UUID userId, String ipAddress) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        Appointment appointment = appointmentRepository.findNextInQueueWithLock(professionalId, startOfDay, endOfDay)
                .orElseThrow(() -> new IllegalStateException("A fila de atendimento está vazia"));

        appointment.transitionTo(AppointmentStatus.CALLING);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .action("CHAMADA_PROXIMO_PACIENTE")
                .appointmentId(savedAppointment.getId())
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(auditLog);

        return savedAppointment;
    }
}
