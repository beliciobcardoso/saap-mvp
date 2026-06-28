package br.com.belloinfo.saap_mvp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades de configuração do namespace saap.* no application.yaml.
 * Fornece metadados para que o IDE reconheça os parâmetros de configuração personalizados.
 */
@Component
@ConfigurationProperties(prefix = "saap")
public class SaapProperties {

    private final App app = new App();
    private final Scheduler scheduler = new Scheduler();

    public App getApp() {
        return app;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public static class App {
        private String baseUrl = "http://localhost:8080";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class Scheduler {
        private final FollowUp followUp = new FollowUp();
        private final Waitlist waitlist = new Waitlist();

        public FollowUp getFollowUp() {
            return followUp;
        }

        public Waitlist getWaitlist() {
            return waitlist;
        }

        public static class FollowUp {
            private String cron = "0 0 * * * *";

            public String getCron() {
                return cron;
            }

            public void setCron(String cron) {
                this.cron = cron;
            }
        }

        public static class Waitlist {
            private String cron = "0 * * * * *";

            public String getCron() {
                return cron;
            }

            public void setCron(String cron) {
                this.cron = cron;
            }
        }
    }
}
