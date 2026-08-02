package com.evidencepilot.config.security;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// in-memory jti allowlist — single-instance prototype. Move to Redis/DB when scaling out.
@Component
public class JwtSessionRegistry {

    private final Set<String> validJtis = ConcurrentHashMap.newKeySet();

    public boolean isValid(String jti) {
        return jti != null && validJtis.contains(jti);
    }

    public void register(String jti) {
        if (jti != null) {
            validJtis.add(jti);
        }
    }

    public void revoke(String jti) {
        if (jti != null) {
            validJtis.remove(jti);
        }
    }
}
