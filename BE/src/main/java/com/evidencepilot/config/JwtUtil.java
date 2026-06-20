package com.evidencepilot.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
 *   <li>Custom claim : {@code role} — the user's {@link com.evidencepilot.domain.enums.UserRole}</li>
 * </ul>
 */
@Component
public class JwtUtil {

    // ── Secret key (min 256 bits / 32 chars for HS256) ──────────────────────────
    // TODO: move to application.yml → ${jwt.secret} before production
    private static final String SECRET_STRING =
            "EvidencePilot-Super-Secret-Key-2025!!";   // exactly 38 chars → 304 bits ✓

    private static final long EXPIRATION_MS = 24L * 60 * 60 * 1000; // 24 hours

    private final SecretKey signingKey;

    public JwtUtil() {
        this.signingKey = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));
    }

    // ── Generate ─────────────────────────────────────────────────────────────────

    /**
     * Builds a signed JWT whose subject is the user's e-mail address and
     * whose {@code role} claim carries the user's role string.
     *
     * @param email the authenticated user's e-mail
     * @param role  the user's role (e.g. "STUDENT", "INSTRUCTOR", "ADMIN")
     * @return compact, signed JWT string (ready to send as {@code Bearer <token>})
     */
    public String generateToken(String email, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
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

    // ── Extract role ──────────────────────────────────────────────────────────────

    /**
     * Extracts the {@code role} custom claim from a verified token.
     *
     * @param token raw JWT string
     * @return the role string (e.g. "ADMIN", "STUDENT", "INSTRUCTOR")
     */
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
