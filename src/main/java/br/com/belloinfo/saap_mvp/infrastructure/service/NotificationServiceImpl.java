package br.com.belloinfo.saap_mvp.infrastructure.service;

import br.com.belloinfo.saap_mvp.application.service.NotificationService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.infrastructure.messaging.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationOrchestrator orchestrator;

    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Override
    public void sendFollowUpNotification(Appointment appointment, String confirmLink, String cancelLink) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled - skipping follow-up notification");
            return;
        }

        var patientEmail = appointment.getPatient().getEmail();
        var message = buildFollowUpMessage(appointment, confirmLink, cancelLink);

        log.info("Sending follow-up notification to patient: {}", appointment.getPatient().getId());
        orchestrator.notifyAll(patientEmail, message);
    }

    @Override
    public void sendWaitlistOfferNotification(WaitlistEntry entry, String acceptLink, String declineLink) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled - skipping waitlist offer notification");
            return;
        }

        var patientEmail = entry.getPatient().getEmail();
        var message = buildWaitlistOfferMessage(entry, acceptLink, declineLink);

        log.info("Sending waitlist offer notification to patient: {}", entry.getPatient().getId());
        orchestrator.notifyAll(patientEmail, message);
    }

    private String buildFollowUpMessage(Appointment appointment, String confirmLink, String cancelLink) {
        return String.format(
            """
                Olá %s,

                Sua consulta está agendada para %s com %s (%s).

                Por favor, confirme sua presença no link abaixo:
                %s

                Se não conseguir comparecer, cancele sua consulta:
                %s

                Atenciosamente,
                SAAP - Sistema de Agendamento e Atendimento de Pacientes
                """,
            appointment.getPatient().getName(),
            appointment.getDateTime(),
            appointment.getProfessional().getName(),
            appointment.getService().getName(),
            confirmLink,
            cancelLink
        );
    }

    private String buildWaitlistOfferMessage(WaitlistEntry entry, String acceptLink, String declineLink) {
        return String.format(
            """
                Olá %s,

                Uma vaga abriu para %s em %s!

                Para aceitar a oferta, clique no link abaixo:
                %s

                Se não puder aproveitar a vaga, clique aqui:
                %s

                Você tem 24 horas para responder.

                Atenciosamente,
                SAAP - Sistema de Agendamento e Atendimento de Pacientes
                """,
            entry.getPatient().getName(),
            entry.getService().getName(),
            entry.getOfferedAppointmentTime(),
            acceptLink,
            declineLink
        );
    }
}
