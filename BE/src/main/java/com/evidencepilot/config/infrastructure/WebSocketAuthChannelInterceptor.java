package com.evidencepilot.config.infrastructure;

import com.evidencepilot.config.security.JwtUtils;
import com.evidencepilot.model.User;
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

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String token = bearerToken(accessor);
        if (token == null || !jwtUtils.validateToken(token)) {
            throw new AccessDeniedException("Invalid WebSocket token");
        }

        UUID userId = jwtUtils.extractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("WebSocket user not found"));
        String role = jwtUtils.extractRole(token);
        String authority = role == null || role.isBlank()
                ? "ROLE_STUDENT"
                : (role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase(Locale.ROOT));

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                user.getId().toString(),
                null,
                List.of(new SimpleGrantedAuthority(authority))));
        return message;
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
}
