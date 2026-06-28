package br.com.belloinfo.saap_mvp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurações clínicas lidas de clinic.settings.* no application.yaml.
 * Controla as janelas de tempo para o follow-up proativo de confirmações.
 */
@Component
@ConfigurationProperties(prefix = "clinic.settings")
public class ClinicSettings {

    /**
     * Janela de antecedência (em horas) para disparar a notificação de follow-up.
     * Agendamentos dentro de agora + confirmationWindowHours e ainda PENDING são notificados.
     * Padrão: 48h
     */
    private long confirmationWindowHours = 48;

    /**
     * Prazo limite de resposta (em horas) antes da consulta.
     * Agendamentos em PENDING_RESPONSE com data dentro deste prazo são processados pelo deadline checker.
     * Padrão: 24h
     */
    private long followUpDeadlineHours = 24;

    /**
     * Se true, agendamentos sem resposta dentro do prazo são cancelados automaticamente.
     * Se false, são marcados como followUpRequired para processamento manual pela recepção.
     * Padrão: true
     */
    private boolean autoCancelAfterNoResponse = true;

    public long getConfirmationWindowHours() {
        return confirmationWindowHours;
    }

    public void setConfirmationWindowHours(long confirmationWindowHours) {
        this.confirmationWindowHours = confirmationWindowHours;
    }

    public long getFollowUpDeadlineHours() {
        return followUpDeadlineHours;
    }

    public void setFollowUpDeadlineHours(long followUpDeadlineHours) {
        this.followUpDeadlineHours = followUpDeadlineHours;
    }

    public boolean isAutoCancelAfterNoResponse() {
        return autoCancelAfterNoResponse;
    }

    public void setAutoCancelAfterNoResponse(boolean autoCancelAfterNoResponse) {
        this.autoCancelAfterNoResponse = autoCancelAfterNoResponse;
    }
}
