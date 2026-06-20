package com.evidencepilot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the Evidence Pilot REST API.
 *
 * <ul>
 * <li>CSRF disabled — stateless REST API; CSRF protection is not needed.</li>
 * <li>CORS enabled — allows local Vite (5173) and CRA (3000) origins.</li>
 * <li>Sessions — STATELESS; all state is carried inside the JWT.</li>
 * <li>Public routes — {@code /api/auth/login},
 * {@code /api/auth/register}.</li>
 * <li>Authenticated routes — {@code /api/auth/update-password},
 * {@code /api/users/me}.</li>
 * <li>Admin routes — {@code /api/users}, {@code /api/users/{id}}
 * (GET/PUT/DELETE)
 * require the {@code ADMIN} role.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CORS — positioned at the beginning, using Customizer.withDefaults()
                // to wire the corsConfigurationSource bean automatically
                .cors(Customizer.withDefaults())

                // 2. Disable CSRF — not needed for stateless JWT REST APIs
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Stateless session — never create an HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Authorization rules — order matters: most specific first
                .authorizeHttpRequests(auth -> auth
                        // Permitting all pre-flight OPTIONS requests globally
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints — no token required
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                // Swagger / OpenAPI docs
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html")
                        .permitAll()

                        // Authenticated auth endpoints — valid JWT required
                        .requestMatchers("/api/auth/update-password").authenticated()

                        // Self-service endpoint — any authenticated user
                        .requestMatchers("/api/users/me").authenticated()

                        // Admin-only user management endpoints
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Every other endpoint requires a valid JWT
                        .anyRequest().authenticated())

                // 5. Insert the JWT filter before Spring's username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // @Bean
    // public CorsConfigurationSource corsConfigurationSource() {
    // CorsConfiguration configuration = new CorsConfiguration();
    // // Allow default Vite (5173) and Create React App (3000) ports.
    // configuration.setAllowedOrigins(List.of(
    // "http://localhost:5173",
    // "http://localhost:3000",
    // "http://127.0.0.1:5173",
    // "http://127.0.0.1:3000",
    // "http://localhost:8080"));
    // configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE",
    // "OPTIONS"));
    // configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    // configuration.setAllowCredentials(true);

    // UrlBasedCorsConfigurationSource source = new
    // UrlBasedCorsConfigurationSource();
    // source.registerCorsConfiguration("/**", configuration);
    // return source;
    // }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use patterns with wildcard to allow any frontend URL during prototyping
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
