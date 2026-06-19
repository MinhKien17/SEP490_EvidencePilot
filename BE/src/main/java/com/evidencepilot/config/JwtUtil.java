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
 * <p><b>Prototype note:</b> the secret is hardcoded for local development only.
 * Before going to production, move it to {@code application.yml} (or a secret
 * manager) and inject it via {@code @Value("${jwt.secret}")}.</p>
 *
 * <ul>
 *   <li>Algorithm : HMAC-SHA256 (HS256)</li>
 *   <li>Expiration : 24 hours</li>
 * </ul>
 */
@Component
public class JwtUtil {

    // ── Secret key (min 256 bits / 32 chars for HS256) ──────────────────────────
    private static final String DEFAULT_SECRET_STRING =
            "EvidencePilot-Super-Secret-Key-2025!!";   // exactly 38 chars → 304 bits ✓

    private static final long DEFAULT_EXPIRATION_MS = 24L * 60 * 60 * 1000; // 24 hours

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret:" + DEFAULT_SECRET_STRING + "}") String secretString,
            @Value("${jwt.expiration-ms:" + DEFAULT_EXPIRATION_MS + "}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ── Generate ─────────────────────────────────────────────────────────────────

    /**
     * Builds a signed JWT whose subject is the user's e-mail address.
     *
     * @param email the authenticated user's e-mail
     * @return compact, signed JWT string (ready to send as {@code Bearer <token>})
     */
    public String generateToken(String email) {
        return generateToken(email, UserRole.STUDENT);
    }

    public String generateToken(String email, UserRole role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)           // defaults to HS256
                .compact();
    }

    // ── Validate ─────────────────────────────────────────────────────────────────

    /**
     * Validates the token's signature and expiry.
     *
     * @param token the raw JWT string (without the "Bearer " prefix)
     * @return {@code true} if the token is valid and not expired; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);   // throws if invalid or expired
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Extract subject (email) ───────────────────────────────────────────────────

    /**
     * Extracts the subject claim (e-mail) from a verified token.
     *
     * @param token raw JWT string
     * @return the e-mail stored in the {@code sub} claim
     */
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
