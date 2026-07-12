package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppNotificationService implements NotificationChannel {

    @Value("${app.notifications.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.notifications.twilio.auth-token:}")
    private String authToken;

    @Value("${app.notifications.twilio.from-number:}")
    private String fromNumber;

    @Override
    @Async
    public void send(String recipient, String message) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot send WhatsApp message: recipient is null or blank");
            return;
        }

        if (!isConfigured()) {
            log.warn("WhatsApp service not configured (missing credentials)");
            return;
        }

        try {
            Twilio.init(accountSid, authToken);
            Message msg = Message.creator(
                    new PhoneNumber("whatsapp:" + recipient),
                    new PhoneNumber("whatsapp:" + fromNumber),
                    message
            ).create();

            log.info("WhatsApp message sent successfully to {} - SID: {}", recipient, msg.getSid());
        } catch (Exception e) {
            log.error("Error sending WhatsApp to {}: {}", recipient, e.getMessage(), e);
        }
    }

    @Async
    public void sendQuickReply(String recipient, String contentSid, Map<String, String> variables) {
        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot send WhatsApp quick-reply: recipient is null or blank");
            return;
        }

        if (!isConfigured() || contentSid == null || contentSid.isBlank()) {
            log.warn("WhatsApp quick-reply not configured (missing credentials or content SID)");
            return;
        }

        try {
            Twilio.init(accountSid, authToken);
            Message msg = Message.creator(
                    new PhoneNumber("whatsapp:" + recipient),
                    new PhoneNumber("whatsapp:" + fromNumber),
                    ""
            ).setContentSid(contentSid)
             .setContentVariables(new ObjectMapper().writeValueAsString(variables))
             .create();

            log.info("WhatsApp quick-reply sent successfully to {} - SID: {}", recipient, msg.getSid());
        } catch (Exception e) {
            log.error("Error sending WhatsApp quick-reply to {}: {}", recipient, e.getMessage(), e);
        }
    }

    private boolean isConfigured() {
        return accountSid != null && !accountSid.isEmpty() &&
               authToken != null && !authToken.isEmpty() &&
               fromNumber != null && !fromNumber.isEmpty();
    }
}
