package com.evidencepilot.config.infrastructure;

import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Slf4j
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String studentEmail;
    private final String studentPassword;
    private final String instructorEmail;
    private final String instructorPassword;

    public DatabaseSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.student.email:}") String studentEmail,
            @Value("${app.student.password:}") String studentPassword,
            @Value("${app.instructor.email:}") String instructorEmail,
            @Value("${app.instructor.password:}") String instructorPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.studentEmail = studentEmail;
        this.studentPassword = studentPassword;
        this.instructorEmail = instructorEmail;
        this.instructorPassword = instructorPassword;
    }

    @Override
    public void run(String... args) {
        seedUser(adminEmail, adminPassword, "Admin", "User", UserRole.ADMIN);
        seedUser(studentEmail, studentPassword, "Test", "Student", UserRole.STUDENT);
        seedUser(instructorEmail, instructorPassword, "Test", "Instructor", UserRole.INSTRUCTOR);
    }

    private void seedUser(String email, String password, String firstName, String lastName, UserRole role) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.info("Skipping {} seed account because its credentials are not configured", role);
            return;
        }
        ensureUser(email.trim().toLowerCase(Locale.ROOT), password, firstName, lastName, role);
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

        log.info("Ensured {} user: {}", role, email);
    }
}
