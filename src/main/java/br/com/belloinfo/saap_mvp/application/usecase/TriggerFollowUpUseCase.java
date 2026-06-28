package br.com.belloinfo.saap_mvp.application.usecase;

import br.com.belloinfo.saap_mvp.application.service.AppointmentActionTokenService;
import br.com.belloinfo.saap_mvp.application.service.NotificationService;
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
 * Dispara notificações de follow-up de confirmação para agendamentos elegíveis.
 *
 * Seleciona agendamentos com status PENDING cujas datas estão dentro da janela de
 * confirmação configurada (clinic.settings.confirmation-window-hours) e que ainda
 * não foram notificados (followUpSentAt IS NULL). Para cada um:
 *  1. Gera tokens JWT de confirmação e cancelamento
 *  2. Dispara notificação simulada (ConsoleNotificationService)
 *  3. Transiciona o status para PENDING_RESPONSE
 *  4. Grava followUpSentAt
 */
@Component
@RequiredArgsConstructor
public class TriggerFollowUpUseCase {

    private static final Logger log = LoggerFactory.getLogger(TriggerFollowUpUseCase.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentActionTokenService tokenService;
    private final NotificationService notificationService;
    private final ClinicSettings clinicSettings;

    @Transactional
    public void execute(String baseUrl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now;
        LocalDateTime windowEnd = now.plusHours(clinicSettings.getConfirmationWindowHours());

        List<Appointment> eligible = appointmentRepository
                .findEligibleForFollowUp(windowStart, windowEnd);

        log.info("Follow-up: {} agendamento(s) elegível(is) para notificação (janela: {} → {})",
                eligible.size(), windowStart, windowEnd);

        for (Appointment appointment : eligible) {
            String confirmToken = tokenService.generateToken(appointment.getId(), "confirm");
            String cancelToken  = tokenService.generateToken(appointment.getId(), "cancel");

            String confirmLink = baseUrl + "/api/v1/appointments/public/confirm?token=" + confirmToken;
            String cancelLink  = baseUrl + "/api/v1/appointments/public/cancel?token=" + cancelToken;

            notificationService.sendFollowUpNotification(appointment, confirmLink, cancelLink);

            appointment.transitionTo(AppointmentStatus.PENDING_RESPONSE);
            appointment.setFollowUpSent(true);
            appointment.setFollowUpSentAt(LocalDateTime.now());
            appointmentRepository.save(appointment);

            log.info("Follow-up enviado para agendamento {} → status PENDING_RESPONSE", appointment.getId());
        }
    }
}
