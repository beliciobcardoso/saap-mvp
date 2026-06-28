package br.com.belloinfo.saap_mvp.infrastructure.scheduler;

import br.com.belloinfo.saap_mvp.application.usecase.ProcessMissedDeadlinesUseCase;
import br.com.belloinfo.saap_mvp.application.usecase.TriggerFollowUpUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler responsável pelo follow-up proativo de confirmações de agendamentos.
 *
 * Dois jobs:
 *  1. sendFollowUpNotifications — a cada hora: envia notificações para PENDING dentro da janela de confirmação
 *  2. processMissedDeadlines    — a cada hora (offset 30min): cancela ou sinaliza PENDING_RESPONSE com prazo expirado
 */
@Component
@RequiredArgsConstructor
public class AppointmentFollowUpScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentFollowUpScheduler.class);

    private final TriggerFollowUpUseCase triggerFollowUpUseCase;
    private final ProcessMissedDeadlinesUseCase processMissedDeadlinesUseCase;

    @Value("${saap.app.base-url:http://localhost:8080}")
    private String baseUrl;

    /** Dispara notificações de follow-up a cada hora no minuto 0. */
    @Scheduled(cron = "${saap.scheduler.follow-up.cron:0 0 * * * *}")
    public void sendFollowUpNotifications() {
        log.info("Iniciando job de envio de notificações de follow-up...");
        try {
            triggerFollowUpUseCase.execute(baseUrl);
            log.info("Job de notificações de follow-up concluído com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao executar job de notificações de follow-up", e);
        }
    }

    /** Processa deadlines expirados a cada hora no minuto 30. */
    @Scheduled(cron = "0 30 * * * *")
    public void processMissedDeadlines() {
        log.info("Iniciando job de processamento de deadlines de follow-up expirados...");
        try {
            processMissedDeadlinesUseCase.execute();
            log.info("Job de deadlines expirados concluído com sucesso.");
        } catch (Exception e) {
            log.error("Erro ao executar job de deadlines de follow-up", e);
        }
    }
}
