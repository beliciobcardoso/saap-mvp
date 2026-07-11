package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Email Orchestrator End-to-End Test")
class NotificationOrchestratorEndToEndTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private NotificationOrchestrator orchestrator;
    private EmailNotificationService emailService;

    @BeforeEach
    void setUp() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(greenMail.getSmtp().getPort());
        mailSender.setDefaultEncoding("UTF-8");

        emailService = new EmailNotificationService(mailSender);
        orchestrator = new NotificationOrchestrator(Arrays.asList(emailService));
    }

    @Test
    @DisplayName("orchestrator sends notification via email channel")
    void orchestrator_sendsViaEmailChannel() throws Exception {
        String recipient = "patient@example.com";
        String message = "Sua consulta foi confirmada para amanhã às 14:00.";

        orchestrator.notifyAll(recipient, message);

        Thread.sleep(500);

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        Message receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getRecipients(Message.RecipientType.TO)[0].toString())
            .contains(recipient);
        assertThat(receivedMessage.getContent().toString())
            .contains(message);
    }

    @Test
    @DisplayName("notification service sends follow-up email")
    void notificationService_sendsFollowUpEmail() throws Exception {
        String testMessage = "Olá João,\n\nSua consulta está agendada para amanhã.\n\n" +
            "Confirme: http://localhost:8080/confirm\n" +
            "Cancele: http://localhost:8080/cancel";

        orchestrator.notifyAll("patient@example.com", testMessage);

        Thread.sleep(500);

        assertThat(greenMail.getReceivedMessages())
            .hasSize(1)
            .allMatch(msg -> {
                try {
                    return msg.getContent().toString().contains("Confirme:");
                } catch (Exception e) {
                    return false;
                }
            });
    }

    @Test
    @DisplayName("orchestrator handles multiple recipients")
    void orchestrator_multipleRecipients() throws Exception {
        String message = "Notificação de teste";

        orchestrator.notifyAll("patient1@example.com", message);
        orchestrator.notifyAll("patient2@example.com", message);
        orchestrator.notifyAll("patient3@example.com", message);

        Thread.sleep(500);

        assertThat(greenMail.getReceivedMessages()).hasSize(3);
    }
}
