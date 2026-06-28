package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import br.com.belloinfo.saap_mvp.application.service.NotificationService;
import br.com.belloinfo.saap_mvp.domain.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationService.class);

    @Override
    public void sendFollowUpNotification(Appointment appointment, String confirmLink, String cancelLink) {
        log.info("=== [NOTIFICAÇÃO ENVIADA] ===");
        log.info("Para: {}", appointment.getPatient().getEmail());
        log.info("Assunto: Confirmação de Consulta - SAAP");
        log.info("Olá, {}, você possui uma consulta marcada para amanhã ({}) com o profissional {}.", 
                appointment.getPatient().getName(), 
                appointment.getDateTime(), 
                appointment.getProfessional().getName());
        log.info("Por favor, confirme ou cancele sua presença através dos links abaixo:");
        log.info("Confirmar presença: {}", confirmLink);
        log.info("Cancelar consulta: {}", cancelLink);
        log.info("=============================");
    }

    @Override
    public void sendWaitlistOfferNotification(br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry entry, String acceptLink, String declineLink) {
        log.info("=== [NOTIFICAÇÃO DE FILA DE ESPERA] ===");
        log.info("Para: {}", entry.getPatient().getEmail());
        log.info("Assunto: Vaga Liberada - SAAP");
        log.info("Olá, {}, um horário com o profissional {} para o serviço {} se liberou.", 
                entry.getPatient().getName(), 
                entry.getProfessional().getName(),
                entry.getService().getName());
        log.info("Você tem 30 minutos para responder. Aceite ou recuse nos links abaixo:");
        log.info("Aceitar vaga: {}", acceptLink);
        log.info("Recusar vaga: {}", declineLink);
        log.info("=======================================");
    }
}
