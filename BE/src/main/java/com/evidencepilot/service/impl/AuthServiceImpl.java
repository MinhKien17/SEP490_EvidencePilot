package com.evidencepilot.service.impl;

import com.evidencepilot.config.security.JwtSessionRegistry;
import com.evidencepilot.config.security.JwtUtils;
import com.evidencepilot.dto.request.LoginRequest;
import com.evidencepilot.dto.request.RegisterRequest;
import com.evidencepilot.dto.response.AuthResponse;
import com.evidencepilot.dto.response.UserResponse;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.AuthService;
import com.evidencepilot.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailVerificationService emailVerificationService;
    private final JwtSessionRegistry sessionRegistry;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(UserRole.STUDENT);
        user.setAccountStatus(AccountStatus.PENDING);
        String verificationToken = emailVerificationService.createVerificationToken(user);

        userRepository.save(user);
        emailVerificationService.sendVerificationEmail(user, verificationToken);

        return new AuthResponse(null, UserResponse.from(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email is not verified");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        String token = jwtUtils.generateToken(user);
        sessionRegistry.register(jwtUtils.extractJti(token));
        return new AuthResponse(token, UserResponse.from(user));
    }

    @Override
    public AuthResponse refresh(String token) {
        if (token == null || !jwtUtils.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        // whole-token lock makes concurrent refresh race-free (one wins, the other 401s)
        synchronized (sessionRegistry) {
            String jti = jwtUtils.extractJti(token);
            if (!sessionRegistry.isValid(jti)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has been revoked");
            }
            User user = userRepository.findById(jwtUtils.extractUserId(token))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
            if (Boolean.FALSE.equals(user.getEmailVerified())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email is not verified");
            }
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
            }

            sessionRegistry.revoke(jti);
            String newToken = jwtUtils.generateToken(user);
            sessionRegistry.register(jwtUtils.extractJti(newToken));
            return new AuthResponse(newToken, UserResponse.from(user));
        }
    }

    @Override
    public String verifyEmail(String token) {
        return emailVerificationService.verifyEmail(token);
    }
}
