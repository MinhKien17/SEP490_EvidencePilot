package com.evidencepilot.service;

import com.evidencepilot.dto.request.AdminUserCreateRequest;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@DataJpaTest
@Import(AdminService.class)
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
        save("active@example.com", "Alice", "One", UserRole.STUDENT, AccountStatus.ACTIVE);
        save("deleted@example.com", "Deleted", "Match", UserRole.STUDENT, AccountStatus.DELETED);
        save("teacher@example.com", "Lin", "Match", UserRole.INSTRUCTOR, AccountStatus.ACTIVE);

        var defaultPage = service.getUsers(0, 20, "createdAt,desc", null, null, null);
        var deletedPage = service.getUsers(0, 20, "createdAt,desc", null, null, AccountStatus.DELETED);
        var queried = service.getUsers(0, 20, "createdAt,desc", "LIN", UserRole.INSTRUCTOR, null);

        assertThat(defaultPage.content()).extracting("email")
                .containsExactlyInAnyOrder("active@example.com", "teacher@example.com");
        assertThat(deletedPage.content()).extracting("email").containsExactly("deleted@example.com");
        assertThat(queried.content()).extracting("email").containsExactly("teacher@example.com");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void setupMailFailureRollsBackNewUser() {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "mail down"))
                .when(passwordResets).requestResetFor(any(User.class));

        assertThatThrownBy(() -> service.createUser(new AdminUserCreateRequest(
                "rollback@example.com", "Roll", "Back", UserRole.STUDENT)))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(users.findByEmail("rollback@example.com")).isEmpty();
    }

    private void save(String email, String firstName, String lastName, UserRole role, AccountStatus status) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setAccountStatus(status);
        user.setEmailVerified(true);
        user.setCreatedAt(java.time.LocalDateTime.now());
        users.save(user);
    }
}
