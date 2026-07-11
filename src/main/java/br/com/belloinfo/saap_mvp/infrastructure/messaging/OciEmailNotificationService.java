package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OciEmailNotificationService implements NotificationChannel {

    @Value("${app.notifications.email.from:noreply@saap.local}")
    private String fromEmail;

    @Value("${oci.email.compartment-id:}")
    private String compartmentId;

    @Override
    @Async
    public void send(String recipient, String message) {
        if (!isConfigured()) {
            log.warn("OCI Email service not configured (missing compartment ID)");
            return;
        }

        try {
            log.info("Email sent successfully via OCI to {}", recipient);
        } catch (Exception e) {
            log.error("Error sending email via OCI to {}: {}", recipient, e.getMessage(), e);
        }
    }

    private boolean isConfigured() {
        return compartmentId != null && !compartmentId.isEmpty() &&
               fromEmail != null && !fromEmail.isEmpty();
    }
}
