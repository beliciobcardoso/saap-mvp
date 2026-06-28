package br.com.belloinfo.saap_mvp.infrastructure.security;

import br.com.belloinfo.saap_mvp.domain.model.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenService {

    private final SecurityProperties securityProperties;

    public TokenService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(securityProperties.getSecurity().getToken().getSecret());
            return JWT.create()
                    .withIssuer("saap-api")
                    .withSubject(user.getEmail())
                    .withClaim("role", user.getRole().name())
                    .withExpiresAt(getExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(securityProperties.getSecurity().getToken().getSecret());
            return JWT.require(algorithm)
                    .withIssuer("saap-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return null; // Token inválido ou expirado
        }
    }

    private Date getExpirationDate() {
        return new Date(System.currentTimeMillis() + securityProperties.getSecurity().getToken().getExpiration());
    }
}
