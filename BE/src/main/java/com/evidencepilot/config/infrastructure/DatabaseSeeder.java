package com.evidencepilot.config.infrastructure;

import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Seeding development users...");

        ensureUser("admin@evidencepilot.dev", "Admin123!", "Admin", "User", UserRole.ADMIN);
        ensureUser("student@evidencepilot.dev", "Student123!", "Test", "Student", UserRole.STUDENT);
        ensureUser("instructor@evidencepilot.dev", "Instructor123!", "Test", "Instructor", UserRole.INSTRUCTOR);

        log.info("Database seeding complete — 3 development users ensured");
    }

    private void ensureUser(String email, String rawPassword, String firstName, String lastName, UserRole role) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiresAt(null);
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        userRepository.save(user);

        log.info("Ensured {} user: {} / {}", role, email, rawPassword);
    }
}
