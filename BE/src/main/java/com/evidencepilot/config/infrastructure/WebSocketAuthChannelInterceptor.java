package com.evidencepilot.config.infrastructure;

import com.evidencepilot.config.security.JwtUtils;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final ConcurrentMap<String, SessionAuthentication> sessions = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        String sessionId = accessor.getSessionId();
        if (accessor.getCommand() == StompCommand.DISCONNECT) {
            if (sessionId != null) {
                sessions.remove(sessionId);
            }
            return message;
        }

        if (accessor.getCommand() != StompCommand.CONNECT) {
            SessionAuthentication session = sessionId == null ? null : sessions.get(sessionId);
            if (session == null) {
                session = sessionAuthentication(accessor.getUser());
            }
            if (session != null) {
                requireCurrentSession(session);
            }
            return message;
        }

        String token = bearerToken(accessor);
        if (token == null || !jwtUtils.validateToken(token)) {
            throw new AccessDeniedException("Invalid WebSocket token");
        }

        UUID userId = jwtUtils.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("WebSocket user not found"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE
                || !Integer.valueOf(user.getTokenVersion()).equals(jwtUtils.extractTokenVersion(token))) {
            throw new AccessDeniedException("Inactive or stale WebSocket token");
        }
        String authority = "ROLE_" + user.getRole().name();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getId().toString(),
                null,
                List.of(new SimpleGrantedAuthority(authority)));
        authentication.setDetails(user.getTokenVersion());
        accessor.setUser(authentication);
        if (sessionId != null) {
            sessions.put(sessionId, new SessionAuthentication(user.getId(), user.getTokenVersion()));
        }
        return message;
    }

    private SessionAuthentication sessionAuthentication(Principal principal) {
        if (principal == null) {
            return null;
        }
        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication)
                || !(authentication.getDetails() instanceof Integer connectedTokenVersion)) {
            throw new AccessDeniedException("Invalid WebSocket session");
        }

        UUID userId;
        try {
            userId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Invalid WebSocket session", exception);
        }
        return new SessionAuthentication(userId, connectedTokenVersion);
    }

    private void requireCurrentSession(SessionAuthentication session) {
        User user = userRepository.findById(session.userId())
                .orElseThrow(() -> new AccessDeniedException("WebSocket user not found"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE
                || user.getTokenVersion() != session.tokenVersion()) {
            throw new AccessDeniedException("Inactive or stale WebSocket session");
        }
    }

    private String bearerToken(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null) {
            authorization = accessor.getFirstNativeHeader("authorization");
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    private record SessionAuthentication(UUID userId, int tokenVersion) {
    }
}
