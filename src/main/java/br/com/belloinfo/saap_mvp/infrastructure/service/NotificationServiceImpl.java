package br.com.belloinfo.saap_mvp.infrastructure.service;

import br.com.belloinfo.saap_mvp.application.service.NotificationService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry;
import br.com.belloinfo.saap_mvp.infrastructure.messaging.EmailNotificationService;
import br.com.belloinfo.saap_mvp.infrastructure.messaging.NotificationOrchestrator;
import br.com.belloinfo.saap_mvp.infrastructure.messaging.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final DateTimeFormatter OFFER_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM 'as' HH:mm");

    private final NotificationOrchestrator orchestrator;
    private final WhatsAppNotificationService whatsAppNotificationService;
    private final EmailNotificationService emailNotificationService;

    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${app.notifications.twilio.waitlist-content-sid:}")
    private String waitlistContentSid;

    @Override
    public void sendFollowUpNotification(Appointment appointment, String confirmLink, String cancelLink) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled - skipping follow-up notification");
            return;
        }

        var patientEmail = appointment.getPatient().getEmail();
        var patientPhone = appointment.getPatient().getPhone();
        var message = buildFollowUpMessage(appointment, confirmLink, cancelLink);

        log.info("Sending follow-up notification to patient: {}", appointment.getPatient().getId());
        sendToAllChannels(patientEmail, patientPhone, message);
    }

    @Override
    public void sendWaitlistOfferNotification(WaitlistEntry entry, String acceptLink, String declineLink) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled - skipping waitlist offer notification");
            return;
        }

        var patientEmail = entry.getPatient().getEmail();
        var patientPhone = entry.getPatient().getPhone();
        var message = buildWaitlistOfferMessage(entry, acceptLink, declineLink);

        log.info("Sending waitlist offer notification to patient: {}", entry.getPatient().getId());
        if (patientEmail != null && !patientEmail.isBlank()) {
            var htmlBody = buildWaitlistOfferHtml(entry, acceptLink, declineLink);
            emailNotificationService.sendHtml(patientEmail, "Vaga disponível - SAAP", htmlBody);
        }

        if (patientPhone != null && !patientPhone.isBlank()) {
            if (waitlistContentSid != null && !waitlistContentSid.isBlank()) {
                whatsAppNotificationService.sendQuickReply(patientPhone, waitlistContentSid, Map.of(
                        "1", entry.getPatient().getName(),
                        "2", entry.getService().getName(),
                        "3", entry.getProfessional().getName(),
                        "4", entry.getOfferedAppointmentTime().format(OFFER_TIME_FORMAT)
                ));
            } else {
                orchestrator.notifyVia("WhatsAppNotificationService", patientPhone, message);
            }
        }
    }

    private void sendToAllChannels(String patientEmail, String patientPhone, String message) {
        orchestrator.notifyVia("EmailNotificationService", patientEmail, message);
        if (patientPhone != null && !patientPhone.isBlank()) {
            orchestrator.notifyVia("WhatsAppNotificationService", patientPhone, message);
        }
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

                Uma vaga abriu para %s com %s em %s!

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
            entry.getProfessional().getName(),
            entry.getOfferedAppointmentTime(),
            acceptLink,
            declineLink
        );
    }

    private String buildWaitlistOfferHtml(WaitlistEntry entry, String acceptLink, String declineLink) {
        return String.format(
            """
                <div style="font-family: Arial, sans-serif; color: #1f2937;">
                  <p>Olá %s,</p>
                  <p>Uma vaga abriu para <strong>%s</strong> com <strong>%s</strong> em %s!</p>
                  <p>Você tem 24 horas para responder.</p>
                  <div style="margin: 24px 0;">
                    <a href="%s" style="background:#16a34a;color:#ffffff;padding:12px 24px;text-decoration:none;border-radius:6px;margin-right:12px;display:inline-block;">Aceitar vaga</a>
                    <a href="%s" style="background:#dc2626;color:#ffffff;padding:12px 24px;text-decoration:none;border-radius:6px;display:inline-block;">Recusar vaga</a>
                  </div>
                  <p>Atenciosamente,<br>SAAP - Sistema de Agendamento e Atendimento de Pacientes</p>
                </div>
                """,
            entry.getPatient().getName(),
            entry.getService().getName(),
            entry.getProfessional().getName(),
            entry.getOfferedAppointmentTime().format(OFFER_TIME_FORMAT),
            acceptLink,
            declineLink
        );
    }
}
