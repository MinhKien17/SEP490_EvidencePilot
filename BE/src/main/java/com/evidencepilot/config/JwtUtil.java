package com.evidencepilot.config;

import com.evidencepilot.domain.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT helper component (JJWT 0.12.x).
 *
 * <p><b>Prototype note:</b> the secret defaults to a local-development value.
 * Override it with {@code jwt.secret} / {@code JWT_SECRET} before production.</p>
 *
 * <ul>
 *   <li>Algorithm: HMAC-SHA256 (HS256)</li>
 *   <li>Expiration: configurable, defaults to 24 hours</li>
 *   <li>Custom claim: {@code role} carries the user's role string</li>
 * </ul>
 */
@Component
public class JwtUtil {

    private static final String DEFAULT_SECRET_STRING =
            "EvidencePilot-Super-Secret-Key-2025!!";

    private static final long DEFAULT_EXPIRATION_MS = 24L * 60 * 60 * 1000;

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret:" + DEFAULT_SECRET_STRING + "}") String secretString,
            @Value("${jwt.expiration-ms:" + DEFAULT_EXPIRATION_MS + "}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        return generateToken(email, UserRole.STUDENT);
    }

    public String generateToken(String email, UserRole role) {
        return generateToken(email, role.name());
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
