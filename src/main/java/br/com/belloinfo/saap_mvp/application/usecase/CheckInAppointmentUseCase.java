package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.AuditLog;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.repository.AuditLogRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.PriorityLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class CheckInAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AuditLogRepository auditLogRepository;
    private static final AtomicLong checkInCounter = new AtomicLong(System.nanoTime());

    @Transactional
    public Appointment execute(UUID appointmentId, PriorityLevel verifiedLevel, UUID verifiedBy, String notes, String ipAddress) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        appointment.checkIn(verifiedLevel, verifiedBy, notes, checkInCounter.incrementAndGet());
        Appointment savedAppointment = appointmentRepository.save(appointment);

        AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .timestamp(LocalDateTime.now())
                .userId(verifiedBy)
                .action("CHECK_IN_VALIDACAO_PRIORIDADE")
                .appointmentId(appointmentId)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(auditLog);

        return savedAppointment;
    }
}
