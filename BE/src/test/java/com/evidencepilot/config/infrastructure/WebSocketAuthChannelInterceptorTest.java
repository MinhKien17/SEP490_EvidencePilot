package com.evidencepilot.config.infrastructure;

import com.evidencepilot.config.security.JwtUtils;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WebSocketAuthChannelInterceptorTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final UserRepository users = mock(UserRepository.class);
    private final WebSocketAuthChannelInterceptor interceptor =
            new WebSocketAuthChannelInterceptor(jwtUtils, users);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void nonConnectMessage_passesThrough() {
        Message<byte[]> message = message(StompCommand.SEND, null);
        assertThat(interceptor.preSend(message, channel)).isSameAs(message);
        verifyNoInteractions(jwtUtils, users);
    }

    @Test
    void connect_rejectsMissingOrInvalidToken() {
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.CONNECT, null), channel))
                .isInstanceOf(AccessDeniedException.class);

        when(jwtUtils.validateToken("bad")).thenReturn(false);
        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.CONNECT, "bad"), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void connect_rejectsUnknownUser() {
        UUID id = UUID.randomUUID();
        when(jwtUtils.validateToken("token")).thenReturn(true);
        when(jwtUtils.extractUserId("token")).thenReturn(id);

        assertThatThrownBy(() -> interceptor.preSend(message(StompCommand.CONNECT, "token"), channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void connect_setsAuthenticatedPrincipalAndRole() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        when(jwtUtils.validateToken("token")).thenReturn(true);
        when(jwtUtils.extractUserId("token")).thenReturn(id);
        when(jwtUtils.extractRole("token")).thenReturn("ADMIN");
        when(users.findById(id)).thenReturn(Optional.of(user));
        Message<byte[]> message = message(StompCommand.CONNECT, "token");

        Message<?> result = interceptor.preSend(message, channel);

        var principal = StompHeaderAccessor.wrap(result).getUser();
        assertThat(principal).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(principal.getName()).isEqualTo(id.toString());
        assertThat(((UsernamePasswordAuthenticationToken) principal).getAuthorities())
                .extracting("authority").containsExactly("ROLE_ADMIN");
    }

    private static Message<byte[]> message(StompCommand command, String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (token != null) {
            accessor.setNativeHeader("Authorization", "Bearer " + token);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
