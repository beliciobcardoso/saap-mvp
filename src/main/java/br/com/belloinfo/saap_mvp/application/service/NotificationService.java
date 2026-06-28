package br.com.belloinfo.saap_mvp.application.service;

import br.com.belloinfo.saap_mvp.domain.model.Appointment;

public interface NotificationService {
    void sendFollowUpNotification(Appointment appointment, String confirmLink, String cancelLink);
    void sendWaitlistOfferNotification(br.com.belloinfo.saap_mvp.domain.model.WaitlistEntry entry, String acceptLink, String declineLink);
}
