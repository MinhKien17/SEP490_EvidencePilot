package com.evidencepilot.config.security;

import com.evidencepilot.model.Session;
import com.evidencepilot.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// DB-backed jti allowlist — survives restarts. Fast in-memory set, write-through on register/revoke.
@Component
public class JwtSessionRegistry {

    private static final long DEFAULT_EXPIRATION_MS = 24L * 60 * 60 * 1000;

    private final Set<String> validJtis = ConcurrentHashMap.newKeySet();
    private final SessionRepository sessions;
    private final long expirationMs;

    // test-only: keeps the registry purely in-memory
    public JwtSessionRegistry() {
        this(null, DEFAULT_EXPIRATION_MS);
    }

    @Autowired
    public JwtSessionRegistry(SessionRepository sessions,
            @Value("${jwt.expiration-ms:" + DEFAULT_EXPIRATION_MS + "}") long expirationMs) {
        this.sessions = sessions;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    void loadFromDatabase() {
        if (sessions == null) return;
        sessions.deleteByExpiresAtBefore(LocalDateTime.now());
        for (Session session : sessions.findAll()) {
            validJtis.add(session.getJti());
        }
    }

    public boolean isValid(String jti) {
        return jti != null && validJtis.contains(jti);
    }

    public void register(String jti) {
        if (jti == null) return;
        validJtis.add(jti);
        if (sessions != null) {
            LocalDateTime now = LocalDateTime.now();
            sessions.save(new Session(jti, null, now, now.plusNanos(expirationMs * 1_000_000)));
        }
    }

    public void revoke(String jti) {
        if (jti == null) return;
        validJtis.remove(jti);
        if (sessions != null) {
            sessions.deleteById(jti);
        }
    }
}