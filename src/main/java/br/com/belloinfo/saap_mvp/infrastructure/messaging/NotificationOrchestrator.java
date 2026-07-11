package br.com.belloinfo.saap_mvp.infrastructure.messaging;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOrchestrator {

    private final List<NotificationChannel> channels;

    public void notifyAll(String recipient, String message) {
        for (NotificationChannel channel : channels) {
            try {
                channel.send(recipient, message);
            } catch (Exception e) {
                log.warn("Error sending notification via {}: {}", channel.getClass().getSimpleName(),
                    e.getMessage());
            }
        }
    }

    public void notifyVia(String channelName, String recipient, String message) {
        channels.stream()
            .filter(c -> c.getClass().getSimpleName().equals(channelName))
            .findFirst()
            .ifPresentOrElse(
                c -> c.send(recipient, message),
                () -> log.warn("Channel {} not found", channelName)
            );
    }
}
