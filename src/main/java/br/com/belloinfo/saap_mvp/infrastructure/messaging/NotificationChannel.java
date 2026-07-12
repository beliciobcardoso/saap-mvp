package br.com.belloinfo.saap_mvp.infrastructure.messaging;

public interface NotificationChannel {
    void send(String recipient, String message);
}
