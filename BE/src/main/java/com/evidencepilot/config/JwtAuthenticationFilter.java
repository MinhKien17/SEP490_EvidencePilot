package com.evidencepilot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Intercepts every incoming request exactly once and validates the JWT
 * present in the {@code Authorization: Bearer <token>} header.
 *
 * <p>If the token is valid the authenticated principal is stored in the
 * {@link SecurityContextHolder} so that downstream security rules can
 * evaluate it.  Invalid or missing tokens are silently ignored here —
 * the {@link SecurityConfig} chain will then reject the request with 401
 * if the endpoint requires authentication.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // 1. Skip if the header is missing or not a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the raw token (strip "Bearer " prefix)
        final String token = authHeader.substring(7);

        // 3. Validate signature + expiry; skip if invalid
        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Only set authentication if the context is still empty
        //    (avoids overwriting an already-authenticated principal)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);
            String authority = "ROLE_" + (role == null ? "STUDENT" : role.toUpperCase(Locale.ROOT));

            // For this prototype we have no UserDetailsService, so we build
            // a minimal authentication token directly from the JWT claims.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,   // credentials — not needed post-authentication
                            List.of(new SimpleGrantedAuthority(authority))
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
