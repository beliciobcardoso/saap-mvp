package br.com.belloinfo.saap_mvp.application.service;

import br.com.belloinfo.saap_mvp.infrastructure.security.SecurityProperties;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class AppointmentActionTokenService {

    private final SecurityProperties securityProperties;
    private static final String ISSUER = "saap-action-token";

    public AppointmentActionTokenService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String generateToken(UUID appointmentId, String action) {
        Algorithm algorithm = Algorithm.HMAC256(securityProperties.getSecurity().getActionToken().getSecret());
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(appointmentId.toString())
                .withClaim("action", action)
                .withExpiresAt(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)) // 24h
                .sign(algorithm);
    }

    public DecodedToken validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(securityProperties.getSecurity().getActionToken().getSecret());
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
            
            UUID appointmentId = UUID.fromString(jwt.getSubject());
            String action = jwt.getClaim("action").asString();
            return new DecodedToken(appointmentId, action);
        } catch (Exception e) {
            throw new IllegalArgumentException("Token de ação inválido ou expirado", e);
        }
    }

    public record DecodedToken(UUID appointmentId, String action) {}
}
