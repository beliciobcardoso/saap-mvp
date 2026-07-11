package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationChannel {

    private final JavaMailSender mailSender;

    @Value("${app.notifications.email.from:noreply@saap.local}")
    private String fromEmail;

    @Override
    @Async
    public void send(String recipient, String message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(recipient);
            mail.setSubject("SAAP Notificação");
            mail.setText(message);

            mailSender.send(mail);
            log.info("Email sent successfully to {}", recipient);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", recipient, e.getMessage(), e);
        }
    }
}
