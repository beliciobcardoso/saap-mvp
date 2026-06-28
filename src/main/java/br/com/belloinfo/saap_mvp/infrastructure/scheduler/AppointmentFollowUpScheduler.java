package br.com.belloinfo.saap_mvp.infrastructure.scheduler;

import br.com.belloinfo.saap_mvp.application.usecase.SendFollowUpNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentFollowUpScheduler {

    private static final Logger log = LoggerFactory.getLogger(AppointmentFollowUpScheduler.class);

    private final SendFollowUpNotificationsUseCase sendFollowUpNotificationsUseCase;

    @Value("${saap.app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Scheduled(cron = "${saap.scheduler.follow-up.cron:0 0 * * * *}")
    public void runFollowUpJob() {
        log.info("Iniciando execução do job agendado de follow-up de confirmações de consultas...");
        try {
            sendFollowUpNotificationsUseCase.execute(baseUrl);
            log.info("Job agendado de follow-up concluído com sucesso!");
        } catch (Exception e) {
            log.error("Erro ao executar o job agendado de follow-up de confirmações", e);
        }
    }
}
