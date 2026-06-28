package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.repository.AppointmentRepository;
import br.com.belloinfo.saap_mvp.domain.valueobject.AppointmentStatus;
import br.com.belloinfo.saap_mvp.infrastructure.config.ClinicSettings;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Processa agendamentos em PENDING_RESPONSE cujo prazo de resposta expirou.
 *
 * Se clinic.settings.auto-cancel-after-no-response = true:
 *   → cancela automaticamente o agendamento (CANCELLED)
 * Se false:
 *   → marca followUpRequired = true para tratamento manual pela recepção
 */
@Component
@RequiredArgsConstructor
public class ProcessMissedDeadlinesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessMissedDeadlinesUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final ClinicSettings clinicSettings;

    @Transactional
    public void execute() {
        LocalDateTime deadline = LocalDateTime.now().plusHours(clinicSettings.getFollowUpDeadlineHours());

        List<Appointment> expired = appointmentRepository.findPendingResponsePastDeadline(deadline);

        log.info("Deadline checker: {} agendamento(s) em PENDING_RESPONSE com prazo expirado (deadline: {})",
                expired.size(), deadline);

        for (Appointment appointment : expired) {
            if (clinicSettings.isAutoCancelAfterNoResponse()) {
                appointment.transitionTo(AppointmentStatus.CANCELLED);
                appointmentRepository.save(appointment);
                log.info("Agendamento {} cancelado automaticamente por não-resposta", appointment.getId());
            } else {
                appointment.setFollowUpRequired(true);
                appointmentRepository.save(appointment);
                log.info("Agendamento {} marcado como followUpRequired para ação manual", appointment.getId());
            }
        }
    }
}
