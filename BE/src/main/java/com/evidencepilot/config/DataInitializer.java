package com.evidencepilot.config;

import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with a default Admin account on first startup.
 *
 * <p>
 * Runs once during application bootstrap via {@link CommandLineRunner}.
 * If the {@code users} table is empty (i.e. after a fresh volume wipe),
 * a single ADMIN user is inserted so that admin-only endpoints remain
 * accessible without manual SQL intervention.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.ADMIN);
            admin.setFirstName("System");
            admin.setLastName("Admin");

            userRepository.save(admin);
            log.info("✅ Default ADMIN account seeded (admin@gmail.com)");
        } else {
            log.info("ℹ️ Users already exist — skipping data seed.");
        }
    }
}
