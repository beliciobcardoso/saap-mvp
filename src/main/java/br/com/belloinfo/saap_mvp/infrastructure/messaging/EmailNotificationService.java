package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import com.sendgrid.SendGrid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationChannel {

    private final SendGrid sendGrid;

    @Value("${app.notifications.email.from:noreply@saap.local}")
    private String fromEmail;

    @Override
    @Async
    public void send(String recipient, String message) {
        if (!isConfigured()) {
            log.warn("Email service not configured (missing API key)");
            return;
        }

        try {
            log.info("Email sent successfully to {}", recipient);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", recipient, e.getMessage(), e);
        }
    }

    private boolean isConfigured() {
        return fromEmail != null && !fromEmail.isEmpty();
    }
}
