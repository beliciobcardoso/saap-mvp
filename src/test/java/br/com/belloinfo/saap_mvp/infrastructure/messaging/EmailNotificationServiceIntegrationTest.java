package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Email Notification Service Integration Tests")
class EmailNotificationServiceIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private EmailNotificationService emailNotificationService;

    @BeforeEach
    void setUp() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(greenMail.getSmtp().getPort());
        mailSender.setDefaultEncoding("UTF-8");

        emailNotificationService = new EmailNotificationService(mailSender);
    }

    @Test
    @DisplayName("sends email successfully via SMTP")
    void send_validEmail_success() throws MessagingException, InterruptedException, java.io.IOException {
        String recipient = "patient@example.com";
        String message = "Test notification message";

        emailNotificationService.send(recipient, message);

        Thread.sleep(500);

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        Message receivedMessage = greenMail.getReceivedMessages()[0];
        assertThat(receivedMessage.getRecipients(Message.RecipientType.TO)[0].toString())
            .contains(recipient);
        assertThat(receivedMessage.getContent().toString())
            .contains(message);
    }

    @Test
    @DisplayName("handles null recipient gracefully")
    void send_nullRecipient_noThrow() {
        assertThat(greenMail.getReceivedMessages()).isEmpty();
        emailNotificationService.send(null, "test message");
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }

    @Test
    @DisplayName("handles blank recipient gracefully")
    void send_blankRecipient_noThrow() {
        assertThat(greenMail.getReceivedMessages()).isEmpty();
        emailNotificationService.send("   ", "test message");
        assertThat(greenMail.getReceivedMessages()).isEmpty();
    }
}
