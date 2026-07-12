package br.com.belloinfo.saap_mvp.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "api")
public class SecurityProperties {

    private static final Logger log = LoggerFactory.getLogger(SecurityProperties.class);
    private static final int MIN_SECRET_LENGTH = 32;

    private final Security security = new Security();

    @PostConstruct
    public void validate() {
        String jwtSecret = security.getToken().getSecret();
        String actionSecret = security.getActionToken().getSecret();

        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "JWT_SECRET deve ter pelo menos " + MIN_SECRET_LENGTH + " caracteres. " +
                "Configure a variável de ambiente JWT_SECRET com um valor seguro."
            );
        }
        if (actionSecret == null || actionSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                "ACTION_TOKEN_SECRET deve ter pelo menos " + MIN_SECRET_LENGTH + " caracteres. " +
                "Configure a variável de ambiente ACTION_TOKEN_SECRET com um valor seguro."
            );
        }
        if (jwtSecret.equals(actionSecret)) {
            throw new IllegalStateException(
                "JWT_SECRET e ACTION_TOKEN_SECRET devem ser diferentes por segurança."
            );
        }
        log.info("Configuração de segurança validada com sucesso.");
    }

    public Security getSecurity() {
        return security;
    }

    public static class Security {
        private final Token token = new Token();
        private final ActionToken actionToken = new ActionToken();
        private final Login login = new Login();

        public Token getToken() {
            return token;
        }

        public ActionToken getActionToken() {
            return actionToken;
        }

        public Login getLogin() {
            return login;
        }

        public static class Token {
            private String secret;
            private Long expiration = 86400000L;

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public Long getExpiration() {
                return expiration;
            }

            public void setExpiration(Long expiration) {
                this.expiration = expiration;
            }
        }

        public static class ActionToken {
            private String secret;
            private Long expiration = 86400000L;

            public String getSecret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }

            public Long getExpiration() {
                return expiration;
            }

            public void setExpiration(Long expiration) {
                this.expiration = expiration;
            }
        }

        public static class Login {
            private String trustedProxies = "";

            public String getTrustedProxies() {
                return trustedProxies;
            }

            public void setTrustedProxies(String trustedProxies) {
                this.trustedProxies = trustedProxies;
            }
        }
    }
}
