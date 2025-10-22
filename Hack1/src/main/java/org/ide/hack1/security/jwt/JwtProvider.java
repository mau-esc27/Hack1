package org.ide.hack1.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.ide.hack1.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtProvider {

    private final String secret;
    private final long expirationSeconds;
    private Algorithm algorithm;
    private JWTVerifier verifier;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expirationSeconds:3600}") long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
        init();
    }

    private void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured. Set JWT_SECRET environment variable or jwt.secret property.");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded != null && decoded.length > 0) {
                this.algorithm = Algorithm.HMAC256(decoded);
            } else {
                this.algorithm = Algorithm.HMAC256(secret);
            }
        } catch (IllegalArgumentException ex) {
            this.algorithm = Algorithm.HMAC256(secret);
        }

        this.verifier = JWT.require(algorithm).build();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusSeconds(expirationSeconds));

        com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withSubject(user.getUsername())
                .withClaim("role", user.getRole() != null ? user.getRole().name() : "")
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt);

        if (user.getBranch() != null) {
            builder.withClaim("branch", user.getBranch());
        }

        return builder.sign(algorithm);
    }

    public boolean validateToken(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException ex) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        DecodedJWT decoded = verifier.verify(token);
        return decoded.getSubject();
    }

    public String getRoleFromToken(String token) {
        DecodedJWT decoded = verifier.verify(token);
        return decoded.getClaim("role").asString();
    }

    public String getBranchFromToken(String token) {
        DecodedJWT decoded = verifier.verify(token);
        return decoded.getClaim("branch").asString();
    }
}
