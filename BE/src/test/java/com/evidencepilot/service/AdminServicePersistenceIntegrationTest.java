package com.evidencepilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.evidencepilot.dto.request.AdminUserImportRequest;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({AdminService.class, AdminServicePersistenceIntegrationTest.JsonConfig.class})
class AdminServicePersistenceIntegrationTest {

    @jakarta.annotation.Resource AdminService service;
    @jakarta.annotation.Resource UserRepository users;
    @MockBean CurrentUserService currentUsers;
    @MockBean PasswordResetService passwordResets;
    @MockBean HealthService health;
    @MockBean AuditService audit;
    @MockBean SystemNotificationService notifications;
    @MockBean PasswordEncoder passwords;

    @BeforeEach
    void passwordHash() {
        when(passwords.encode(any())).thenReturn("$2a$10$random");
    }

    @Test
    void defaultListExcludesDeletedWhileExplicitDeletedAndQueryRemainAvailable() {
        User active = save("active@example.com", "Alice", "One", UserRole.STUDENT, AccountStatus.ACTIVE);
        active.setStudentCode("SE170608");
        users.save(active);
        save("deleted@example.com", "Deleted", "Match", UserRole.STUDENT, AccountStatus.DELETED);
        save("teacher@example.com", "Lin", "Match", UserRole.INSTRUCTOR, AccountStatus.ACTIVE);

        var defaultPage = service.getUsers(0, 20, "createdAt,desc", null, null, null);
        var deletedPage = service.getUsers(0, 20, "createdAt,desc", null, null, AccountStatus.DELETED);
        var queried = service.getUsers(0, 20, "createdAt,desc", "LIN", UserRole.INSTRUCTOR, null);
        var queriedByCode = service.getUsers(0, 20, "createdAt,desc", "se170608", UserRole.STUDENT, null);

        assertThat(defaultPage.content()).extracting("email")
                .containsExactlyInAnyOrder("active@example.com", "teacher@example.com");
        assertThat(deletedPage.content()).extracting("email").containsExactly("deleted@example.com");
        assertThat(queried.content()).extracting("email").containsExactly("teacher@example.com");
        assertThat(queriedByCode.content()).extracting("studentCode").containsExactly("SE170608");
    }

    @Test
    void invalidImportDoesNotUpdateAnyRow() {
        User existing = save("existing@example.com", "Original", "Student", UserRole.STUDENT, AccountStatus.ACTIVE);
        existing.setStudentCode("SE1");
        users.save(existing);
        User owner = save("owner@example.com", "Code", "Owner", UserRole.STUDENT, AccountStatus.ACTIVE);
        owner.setStudentCode("SE2");
        users.saveAndFlush(owner);

        var response = service.importUsers(new AdminUserImportRequest("STUDENT", List.of(
                new AdminUserImportRequest.UserItem("existing@example.com", "Changed", "Student", "SE3"),
                new AdminUserImportRequest.UserItem("new@example.com", "New", "Student", "SE2"))));

        assertThat(response.errors()).isNotEmpty();
        User unchanged = users.findByEmail("existing@example.com").orElseThrow();
        assertThat(unchanged.getFirstName()).isEqualTo("Original");
        assertThat(unchanged.getStudentCode()).isEqualTo("SE1");
        assertThat(users.findByEmail("new@example.com")).isEmpty();
    }

    private User save(String email, String firstName, String lastName, UserRole role, AccountStatus status) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setAccountStatus(status);
        user.setCreatedAt(java.time.LocalDateTime.now());
        return users.save(user);
    }

    @TestConfiguration
    static class JsonConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
