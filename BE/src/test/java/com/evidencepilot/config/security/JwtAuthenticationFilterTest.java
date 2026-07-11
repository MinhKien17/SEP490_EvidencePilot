package com.evidencepilot.config.security;

import com.evidencepilot.model.User;
import com.evidencepilot.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtils, users);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingBearerToken_continuesWithoutAuthentication() throws Exception {
        var request = new MockHttpServletRequest();
        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtils, users);
    }

    @Test
    void invalidToken_continuesWithoutAuthentication() throws Exception {
        var request = request("bad");
        when(jwtUtils.validateToken("bad")).thenReturn(false);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(users);
    }

    @Test
    void unknownUser_continuesWithoutAuthentication() throws Exception {
        UUID userId = UUID.randomUUID();
        var request = request("valid");
        when(jwtUtils.validateToken("valid")).thenReturn(true);
        when(jwtUtils.extractUserId("valid")).thenReturn(userId);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validToken_setsUserAndNormalizedAuthority() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        var request = request("valid");
        when(jwtUtils.validateToken("valid")).thenReturn(true);
        when(jwtUtils.extractUserId("valid")).thenReturn(userId);
        when(jwtUtils.extractRole("valid")).thenReturn("instructor");
        when(users.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_INSTRUCTOR");
    }

    private static MockHttpServletRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
