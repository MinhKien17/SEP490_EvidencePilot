package com.evidencepilot.service;

import com.evidencepilot.dto.request.AdminBroadcastRequest;
import com.evidencepilot.dto.request.AdminUserCreateRequest;
import com.evidencepilot.dto.request.AdminUserRoleRequest;
import com.evidencepilot.dto.request.AdminUserStatusRequest;
import com.evidencepilot.model.AuditLog;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.AuditLogRepository;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UserRepository users;
    @Mock ProjectRepository projects;
    @Mock SourceCategoryRepository categories;
    @Mock CollectionRepository collections;
    @Mock DocumentRepository documents;
    @Mock AuditLogRepository auditLogs;
    @Mock CurrentUserService currentUsers;
    @Mock PasswordResetService passwordResets;
    @Mock HealthService health;
    @Mock AuditService audit;
    @Mock SystemNotificationService notifications;
    @Mock PasswordEncoder passwords;
    @InjectMocks AdminService service;

    @Test
    void createsPendingUnverifiedUserThenStartsSetupAndAuditsSafeFields() {
        User admin = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        when(currentUsers.requireCurrentUser()).thenReturn(admin);
        when(users.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwords.encode(any())).thenReturn("$2a$10$random");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var response = service.createUser(new AdminUserCreateRequest(
                "new@example.com", "New", "User", UserRole.INSTRUCTOR));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(saved.getEmailVerified()).isFalse();
        assertThat(saved.getPasswordHash()).startsWith("$2a$");
        verify(passwordResets).requestResetFor(saved);
        ArgumentCaptor<Object> auditValue = ArgumentCaptor.forClass(Object.class);
        verify(audit).record(eq("USER_CREATED"), eq("USER"), eq(saved.getId()), eq(admin),
                isNull(), auditValue.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> safeAuditValue = (Map<String, Object>) auditValue.getValue();
        assertThat(safeAuditValue).containsOnlyKeys(
                "email", "role", "accountStatus", "emailVerified", "firstName", "lastName");
        assertThat(auditValue.getValue().toString()).doesNotContainIgnoringCase(
                "password", "token", "reset", "link");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(java.util.Arrays.stream(response.getClass().getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("passwordHash", "passwordResetTokenHash");
    }

    @Test
    void rejectsDuplicateAndInvalidCreationRoles() {
        when(users.existsByEmailIgnoreCase("duplicate@example.com")).thenReturn(true);
        assertConflict(() -> service.createUser(new AdminUserCreateRequest(
                "duplicate@example.com", "D", "U", UserRole.STUDENT)));
        assertConflict(() -> service.createUser(new AdminUserCreateRequest(
                "admin@example.com", "A", "D", UserRole.ADMIN)));
        verifyNoInteractions(passwordResets, audit);
    }

    @Test
    void roleStatusAndDeleteMutationsProtectAdminsAndIncrementTokenVersion() {
        User target = user(UserRole.STUDENT, AccountStatus.ACTIVE);
        target.setTokenVersion(4);
        User admin = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        when(currentUsers.requireCurrentUser()).thenReturn(admin);
        when(users.findById(target.getId())).thenReturn(Optional.of(target));
        when(users.save(target)).thenReturn(target);

        service.updateRole(target.getId(), new AdminUserRoleRequest(UserRole.INSTRUCTOR));
        assertThat(target.getRole()).isEqualTo(UserRole.INSTRUCTOR);
        assertThat(target.getTokenVersion()).isEqualTo(5);
        verify(audit).record(eq("USER_ROLE_UPDATED"), eq("USER"), eq(target.getId()), eq(admin),
                any(Map.class), any(Map.class));

        service.updateStatus(target.getId(), new AdminUserStatusRequest(AccountStatus.BANNED));
        assertThat(target.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
        assertThat(target.getTokenVersion()).isEqualTo(6);

        target.setPasswordResetTokenHash("secret");
        target.setPasswordResetTokenExpiresAt(LocalDateTime.now());
        target.setPasswordResetRequestedAt(LocalDateTime.now());
        service.deleteUser(target.getId());
        assertThat(target.getAccountStatus()).isEqualTo(AccountStatus.DELETED);
        assertThat(target.getTokenVersion()).isEqualTo(7);
        assertThat(target.getPasswordResetTokenHash()).isNull();
        assertThat(target.getPasswordResetTokenExpiresAt()).isNull();
        assertThat(target.getPasswordResetRequestedAt()).isNull();
    }

    @Test
    void immutableAdminAndNoopOrInvalidChangesConflict() {
        User admin = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));
        assertConflict(() -> service.updateRole(admin.getId(), new AdminUserRoleRequest(UserRole.STUDENT)));
        assertConflict(() -> service.updateStatus(admin.getId(), new AdminUserStatusRequest(AccountStatus.BANNED)));
        assertConflict(() -> service.deleteUser(admin.getId()));

        User student = user(UserRole.STUDENT, AccountStatus.ACTIVE);
        when(users.findById(student.getId())).thenReturn(Optional.of(student));
        assertConflict(() -> service.updateRole(student.getId(), new AdminUserRoleRequest(UserRole.STUDENT)));
        assertConflict(() -> service.updateRole(student.getId(), new AdminUserRoleRequest(UserRole.ADMIN)));
        assertConflict(() -> service.updateStatus(student.getId(), new AdminUserStatusRequest(AccountStatus.ACTIVE)));
        assertConflict(() -> service.updateStatus(student.getId(), new AdminUserStatusRequest(AccountStatus.DELETED)));

        User invalidRoleTarget = new User();
        invalidRoleTarget.setId(UUID.randomUUID());
        invalidRoleTarget.setAccountStatus(AccountStatus.ACTIVE);
        when(users.findById(invalidRoleTarget.getId())).thenReturn(Optional.of(invalidRoleTarget));
        assertConflict(() -> service.updateRole(invalidRoleTarget.getId(), new AdminUserRoleRequest(UserRole.STUDENT)));

        User deletedTarget = user(UserRole.STUDENT, AccountStatus.DELETED);
        when(users.findById(deletedTarget.getId())).thenReturn(Optional.of(deletedTarget));
        assertConflict(() -> service.updateStatus(deletedTarget.getId(), new AdminUserStatusRequest(AccountStatus.ACTIVE)));
        verify(users, never()).save(any());
    }

    @Test
    void adminResetUsesStrictServiceWithoutChangingTokenVersionAndAuditsOnlyIssuedReset() {
        User target = user(UserRole.INSTRUCTOR, AccountStatus.BANNED);
        User admin = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        target.setTokenVersion(3);
        when(users.findById(target.getId())).thenReturn(Optional.of(target));
        when(currentUsers.requireCurrentUser()).thenReturn(admin);
        org.mockito.Mockito.doAnswer(invocation -> {
            target.setPasswordResetRequestedAt(LocalDateTime.now());
            return null;
        }).when(passwordResets).requestResetFor(target);

        service.requestPasswordReset(target.getId());

        assertThat(target.getTokenVersion()).isEqualTo(3);
        verify(passwordResets).requestResetFor(target);
        verify(audit).record("PASSWORD_RESET_REQUESTED", "USER", target.getId(), admin, null,
                Map.of("email", target.getEmail()));
    }

    @Test
    void adminResetRejectsAdminAndDeletedAndDoesNotAuditCooldownSuppression() {
        User adminTarget = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        User deleted = user(UserRole.STUDENT, AccountStatus.DELETED);
        when(users.findById(adminTarget.getId())).thenReturn(Optional.of(adminTarget));
        when(users.findById(deleted.getId())).thenReturn(Optional.of(deleted));

        assertConflict(() -> service.requestPasswordReset(adminTarget.getId()));
        assertConflict(() -> service.requestPasswordReset(deleted.getId()));

        User coolingDown = user(UserRole.STUDENT, AccountStatus.ACTIVE);
        coolingDown.setPasswordResetRequestedAt(LocalDateTime.now());
        when(users.findById(coolingDown.getId())).thenReturn(Optional.of(coolingDown));
        service.requestPasswordReset(coolingDown.getId());

        verify(passwordResets).requestResetFor(coolingDown);
        verifyNoInteractions(audit);
    }

    @Test
    void dashboardIncludesEveryEnumAndDelegatesReadinessExactly() {
        Map<String, Object> readiness = Map.of("status", "UP");
        when(users.count()).thenReturn(9L);
        when(users.countByRole()).thenReturn(List.<Object[]>of(new Object[]{UserRole.STUDENT, 5L}));
        when(users.countByAccountStatus()).thenReturn(List.<Object[]>of(new Object[]{AccountStatus.ACTIVE, 4L}));
        when(projects.countByActiveTrue()).thenReturn(3L);
        when(projects.countActiveByStatus()).thenReturn(List.<Object[]>of(new Object[]{ProjectStatus.ASSIGNED, 2L}));
        when(categories.countByActiveTrue()).thenReturn(7L);
        when(collections.countByActiveTrue()).thenReturn(6L);
        when(documents.countByActiveTrueAndDocType(DocumentType.SOURCE)).thenReturn(11L);
        when(documents.countByActiveTrueAndDocType(DocumentType.PAPER)).thenReturn(12L);
        when(health.checkReadiness()).thenReturn(readiness);

        var result = service.getDashboard();

        assertThat(result.usersByRole()).containsOnlyKeys(UserRole.values());
        assertThat(result.usersByStatus()).containsOnlyKeys(AccountStatus.values());
        assertThat(result.activeProjectsByStatus()).containsOnlyKeys(ProjectStatus.values());
        assertThat(result.usersByRole().get(UserRole.ADMIN)).isZero();
        assertThat(result.infrastructureReadiness()).isSameAs(readiness);
    }

    @Test
    void auditLogsAreNewestFirstAndSupportActorOrCompleteEntityLookup() {
        Pageable anyPage = Pageable.ofSize(20);
        AuditLog log = new AuditLog();
        User actor = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        log.setActor(actor);
        log.setAction("USER_CREATED");
        log.setEntityType("USER");
        log.setEntityId(UUID.randomUUID());
        log.setOccurredAt(LocalDateTime.now());
        when(auditLogs.findByActorIdOrderByOccurredAtDesc(eq(actor.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), anyPage, 1));

        var response = service.getAuditLogs(0, 20, actor.getId(), null, null);

        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.actorId()).isEqualTo(actor.getId());
            assertThat(item.actorEmail()).isEqualTo(actor.getEmail());
        });
        verify(auditLogs).findByActorIdOrderByOccurredAtDesc(eq(actor.getId()), any(Pageable.class));
    }

    @Test
    void auditLogsCombineActorAndCompleteEntityFilters() {
        User actor = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        UUID entityId = UUID.randomUUID();
        AuditLog combined = auditLog(actor, "COMBINED", entityId);
        when(auditLogs.findByActorIdAndEntityTypeAndEntityIdOrderByOccurredAtDesc(
                eq(actor.getId()), eq("USER"), eq(entityId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(combined)));

        var response = service.getAuditLogs(0, 20, actor.getId(), "USER", entityId);

        assertThat(response.content()).singleElement().extracting("action").isEqualTo("COMBINED");
    }

    @Test
    void broadcastTargetsOnlyActiveRolePersistsAndPushesOncePerRecipientAndAuditsOnce() {
        User admin = user(UserRole.ADMIN, AccountStatus.ACTIVE);
        User first = user(UserRole.STUDENT, AccountStatus.ACTIVE);
        User second = user(UserRole.STUDENT, AccountStatus.ACTIVE);
        when(currentUsers.requireCurrentUser()).thenReturn(admin);
        when(users.findByAccountStatusAndRole(AccountStatus.ACTIVE, UserRole.STUDENT))
                .thenReturn(List.of(first, second));

        long count = service.broadcast(new AdminBroadcastRequest("Maintenance soon", UserRole.STUDENT));

        assertThat(count).isEqualTo(2);
        verify(notifications).createNotification(first, admin, "ADMIN_BROADCAST", null, "Maintenance soon");
        verify(notifications).createNotification(second, admin, "ADMIN_BROADCAST", null, "Maintenance soon");
        verify(audit).record(eq("NOTIFICATION_BROADCAST"), eq("SYSTEM_NOTIFICATION"), isNull(), eq(admin),
                isNull(), any(Map.class));
    }

    private User user(UserRole role, AccountStatus status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.com");
        user.setRole(role);
        user.setAccountStatus(status);
        user.setEmailVerified(true);
        user.setPasswordHash("hash");
        return user;
    }

    private AuditLog auditLog(User actor, String action, UUID entityId) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType("USER");
        log.setEntityId(entityId);
        log.setOccurredAt(LocalDateTime.now());
        return log;
    }

    private void assertConflict(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(409));
    }
}
