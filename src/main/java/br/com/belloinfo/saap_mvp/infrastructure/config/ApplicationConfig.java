package br.com.belloinfo.saap_mvp.infrastructure.config;

import com.sendgrid.SendGrid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class ApplicationConfig {

    @Bean
    public SendGrid sendGrid(@Value("${app.notifications.sendgrid.api-key:}") String apiKey) {
        return new SendGrid(apiKey);
    }
}
