package com.evidencepilot.config;

import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = normalize(adminEmail);
        String password = normalize(adminPassword);
        if (email == null || password == null) {
            return;
        }

        User admin = userRepository.findByEmail(email).orElseGet(User::new);
        admin.setEmail(email);
        admin.setRole(UserRole.ADMIN);
        admin.setEmailVerified(true);
        admin.setEmailVerificationTokenHash(null);
        admin.setEmailVerificationTokenExpiresAt(null);
        admin.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(admin);
        log.info("Default admin {}", email);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
