package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot send email: recipient is null or blank");
            return;
        }

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

    @Async
    public void sendHtml(String recipient, String subject, String htmlBody) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot send email: recipient is null or blank");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
            log.info("HTML email sent successfully to {}", recipient);
        } catch (Exception e) {
            log.error("Error sending HTML email to {}: {}", recipient, e.getMessage(), e);
        }
    }
}
