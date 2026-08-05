package com.evidencepilot.config.infrastructure;

import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.AccountStatus;
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
    private final String adminFirstName;
    private final String adminLastName;
    private final String studentEmail;
    private final String studentPassword;
    private final String studentFirstName;
    private final String studentLastName;
    private final String studentCode;
    private final String instructorEmail;
    private final String instructorPassword;
    private final String instructorFirstName;
    private final String instructorLastName;

    public DatabaseSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword,
            @Value("${app.admin.first-name:Admin}") String adminFirstName,
            @Value("${app.admin.last-name:User}") String adminLastName,
            @Value("${app.student.email:}") String studentEmail,
            @Value("${app.student.password:}") String studentPassword,
            @Value("${app.student.first-name:Test}") String studentFirstName,
            @Value("${app.student.last-name:Student}") String studentLastName,
            @Value("${app.student.student-code:}") String studentCode,
            @Value("${app.instructor.email:}") String instructorEmail,
            @Value("${app.instructor.password:}") String instructorPassword,
            @Value("${app.instructor.first-name:Test}") String instructorFirstName,
            @Value("${app.instructor.last-name:Instructor}") String instructorLastName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminFirstName = adminFirstName;
        this.adminLastName = adminLastName;
        this.studentEmail = studentEmail;
        this.studentPassword = studentPassword;
        this.studentFirstName = studentFirstName;
        this.studentLastName = studentLastName;
        this.studentCode = studentCode;
        this.instructorEmail = instructorEmail;
        this.instructorPassword = instructorPassword;
        this.instructorFirstName = instructorFirstName;
        this.instructorLastName = instructorLastName;
    }

    @Override
    public void run(String... args) {
        seedUser(adminEmail, adminPassword, adminFirstName, adminLastName, UserRole.ADMIN, null);
        seedUser(studentEmail, studentPassword, studentFirstName, studentLastName, UserRole.STUDENT, studentCode);
        seedUser(instructorEmail, instructorPassword, instructorFirstName, instructorLastName, UserRole.INSTRUCTOR, null);
    }

    private void seedUser(
            String email,
            String password,
            String firstName,
            String lastName,
            UserRole role,
            String studentCode) {
        if (email == null || email.isBlank()
                || password == null || password.isBlank()
                || firstName == null || firstName.isBlank()
                || lastName == null || lastName.isBlank()
                || (role == UserRole.STUDENT && (studentCode == null || studentCode.isBlank()))) {
            log.info("Skipping {} seed account because its required fields are not configured", role);
            return;
        }
        ensureUser(
                email.trim().toLowerCase(Locale.ROOT),
                password,
                firstName.trim(),
                lastName.trim(),
                role,
                role == UserRole.STUDENT ? studentCode.trim().toUpperCase(Locale.ROOT) : null);
    }

    private void ensureUser(
            String email,
            String rawPassword,
            String firstName,
            String lastName,
            UserRole role,
            String studentCode) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setStudentCode(studentCode);
        user.setAccountStatus(AccountStatus.ACTIVE);
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        userRepository.save(user);

        log.info("Ensured {} user: {}", role, email);
    }
}
