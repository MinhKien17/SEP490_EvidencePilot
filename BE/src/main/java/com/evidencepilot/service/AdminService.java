package com.evidencepilot.service;

import com.evidencepilot.dto.request.AdminBroadcastRequest;
import com.evidencepilot.dto.request.AdminUserCreateRequest;
import com.evidencepilot.dto.request.AdminUserRoleRequest;
import com.evidencepilot.dto.request.AdminUserStatusRequest;
import com.evidencepilot.dto.request.PagingRequest;
import com.evidencepilot.dto.response.AdminAuditLogResponse;
import com.evidencepilot.dto.response.AdminDashboardResponse;
import com.evidencepilot.dto.response.AdminUserResponse;
import com.evidencepilot.dto.response.PagedResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.Document;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.model.enums.DocumentType;
import com.evidencepilot.model.enums.ProcessingStatus;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.AuditLogRepository;
import com.evidencepilot.repository.CollectionRepository;
import com.evidencepilot.repository.DocumentRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.SourceCategoryRepository;
import com.evidencepilot.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Set<String> USER_SORT_FIELDS = Set.of("createdAt", "email", "firstName", "lastName", "role", "accountStatus");

    private final UserRepository users;
    private final ProjectRepository projects;
    private final SourceCategoryRepository categories;
    private final CollectionRepository collections;
    private final DocumentRepository documents;
    private final AuditLogRepository auditLogs;
    private final CurrentUserService currentUsers;
    private final PasswordResetService passwordResets;
    private final HealthService health;
    private final AuditService audit;
    private final SystemNotificationService notifications;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwords;

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> getUsers(
            int page, int size, String sort, String q, UserRole role, AccountStatus status) {
        Pageable pageable = PagingRequest.pageable(page, size, sort, USER_SORT_FIELDS, "createdAt,desc");
        Specification<User> filters = (root, query, builder) -> builder.conjunction();
        filters = filters.and((root, query, builder) -> status == null
                ? builder.notEqual(root.get("accountStatus"), AccountStatus.DELETED)
                : builder.equal(root.get("accountStatus"), status));
        if (role != null) {
            filters = filters.and((root, query, builder) -> builder.equal(root.get("role"), role));
        }
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            filters = filters.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("email")), pattern),
                    builder.like(builder.lower(root.get("firstName")), pattern),
                    builder.like(builder.lower(root.get("lastName")), pattern)));
        }
        return PagedResponse.from(users.findAll(filters, pageable).map(AdminUserResponse::from));
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        requireManagedRole(request.role());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw conflict("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(request.role());
        user.setAccountStatus(AccountStatus.PENDING);
        user.setEmailVerified(false);
        user.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
        user.setCreatedAt(LocalDateTime.now());
        user = users.save(user);

        passwordResets.requestResetFor(user);
        audit.record("USER_CREATED", "USER", user.getId(), currentUsers.requireCurrentUser(), null, safeUser(user));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateRole(UUID id, AdminUserRoleRequest request) {
        User user = requireMutableUser(id);
        requireManagedRole(request.role());
        if (user.getRole() == request.role()) {
            throw conflict("Role is unchanged");
        }
        UserRole oldRole = user.getRole();
        user.setRole(request.role());
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);
        audit.record("USER_ROLE_UPDATED", "USER", id, currentUsers.requireCurrentUser(),
                Map.of("role", oldRole), Map.of("role", user.getRole()));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID id, AdminUserStatusRequest request) {
        User user = requireMutableUser(id);
        if (user.getAccountStatus() == AccountStatus.DELETED
                || request.status() == AccountStatus.DELETED
                || user.getAccountStatus() == request.status()) {
            throw conflict("Status change is not allowed");
        }
        AccountStatus oldStatus = user.getAccountStatus();
        user.setAccountStatus(request.status());
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);
        audit.record("USER_STATUS_UPDATED", "USER", id, currentUsers.requireCurrentUser(),
                Map.of("accountStatus", oldStatus), Map.of("accountStatus", user.getAccountStatus()));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = requireMutableUser(id);
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw conflict("User is already deleted");
        }
        AccountStatus oldStatus = user.getAccountStatus();
        user.setAccountStatus(AccountStatus.DELETED);
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetTokenExpiresAt(null);
        user.setPasswordResetRequestedAt(null);
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);
        audit.record("USER_DELETED", "USER", id, currentUsers.requireCurrentUser(),
                Map.of("accountStatus", oldStatus), Map.of("accountStatus", AccountStatus.DELETED));
    }

    @Transactional
    public void requestPasswordReset(UUID id) {
        User user = requireUser(id);
        if (user.getRole() == UserRole.ADMIN || user.getAccountStatus() == AccountStatus.DELETED) {
            throw conflict("Account is not eligible for password reset");
        }
        LocalDateTime previousRequest = user.getPasswordResetRequestedAt();
        passwordResets.requestResetFor(user);
        if (!Objects.equals(previousRequest, user.getPasswordResetRequestedAt())) {
            audit.record("PASSWORD_RESET_REQUESTED", "USER", id, currentUsers.requireCurrentUser(), null,
                    Map.of("email", user.getEmail()));
        }
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        EnumMap<UserRole, Long> usersByRole = zeroes(UserRole.class);
        for (Object[] row : users.countByRole()) {
            usersByRole.put((UserRole) row[0], ((Number) row[1]).longValue());
        }
        EnumMap<AccountStatus, Long> usersByStatus = zeroes(AccountStatus.class);
        for (Object[] row : users.countByAccountStatus()) {
            usersByStatus.put((AccountStatus) row[0], ((Number) row[1]).longValue());
        }
        EnumMap<ProjectStatus, Long> projectsByStatus = zeroes(ProjectStatus.class);
        for (Object[] row : projects.countActiveByStatus()) {
            projectsByStatus.put((ProjectStatus) row[0], ((Number) row[1]).longValue());
        }
        return new AdminDashboardResponse(
                users.count(), usersByRole, usersByStatus,
                projects.countByActiveTrue(), projectsByStatus,
                categories.countByActiveTrue(), collections.countByActiveTrue(),
                documents.countByActiveTrueAndDocType(DocumentType.SOURCE),
                documents.countByActiveTrueAndDocType(DocumentType.PAPER),
                health.checkReadiness());
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminAuditLogResponse> getAuditLogs(
            int page, int size, UUID actorId, String entityType, UUID entityId) {
        Pageable pageable = PagingRequest.pageable(page, size, "occurredAt,desc", Set.of("occurredAt"), "occurredAt,desc");
        Page<com.evidencepilot.model.AuditLog> result;
        if (actorId != null && entityType != null && !entityType.isBlank() && entityId != null) {
            result = auditLogs.findByActorIdAndEntityTypeAndEntityIdOrderByOccurredAtDesc(
                    actorId, entityType, entityId, pageable);
        } else if (actorId != null) {
            result = auditLogs.findByActorIdOrderByOccurredAtDesc(actorId, pageable);
        } else if (entityType != null && !entityType.isBlank() && entityId != null) {
            result = auditLogs.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId, pageable);
        } else {
            result = auditLogs.findAllByOrderByOccurredAtDesc(pageable);
        }
        return PagedResponse.from(result.map(AdminAuditLogResponse::from));
    }

    @Transactional
    public long broadcast(AdminBroadcastRequest request) {
        User actor = currentUsers.requireCurrentUser();
        List<User> recipients = request.role() == null
                ? users.findByAccountStatus(AccountStatus.ACTIVE)
                : users.findByAccountStatusAndRole(AccountStatus.ACTIVE, request.role());
        for (User recipient : recipients) {
            notifications.createNotification(recipient, actor, "ADMIN_BROADCAST", null, request.message());
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("role", request.role());
        metadata.put("recipientCount", recipients.size());
        audit.record("NOTIFICATION_BROADCAST", "SYSTEM_NOTIFICATION", null, actor, null, metadata);
        return recipients.size();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getExtractionQueue() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ProcessingStatus s : ProcessingStatus.values()) {
            counts.put(s.name(), documents.countByProcessingStatus(s));
        }
        List<Document> failed = documents.findByProcessingStatusAndActiveTrue(ProcessingStatus.FAILED);
        List<Map<String, Object>> failedList = failed.stream().limit(50).<Map<String, Object>>map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("originalFilename", d.getOriginalFilename());
            m.put("processingError", d.getProcessingError());
            m.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
            return m;
        }).toList();
        return Map.of("counts", counts, "failed", failedList);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBroadcastHistory() {
        Pageable pageable = PagingRequest.pageable(0, 20, "occurredAt,desc", Set.of("occurredAt"), "occurredAt,desc");
        return auditLogs.findByActionOrderByOccurredAtDesc("NOTIFICATION_BROADCAST", pageable)
                .map(log -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("actorEmail", log.getActor() != null ? log.getActor().getEmail() : null);
                    m.put("occurredAt", log.getOccurredAt() != null ? log.getOccurredAt().toString() : null);
                    try {
                        m.put("details", log.getNewValue() != null
                                ? objectMapper.readValue(log.getNewValue(), Map.class)
                                : null);
                    } catch (Exception e) {
                        m.put("details", log.getNewValue());
                    }
                    return m;
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCollections() {
        return collections.findAll().stream()
                .filter(c -> c.isActive() || c.getInstructor() != null)
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("name", c.getTitle());
                    m.put("description", c.getDescription());
                    m.put("instructorEmail", c.getInstructor().getEmail());
                    m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
                    m.put("active", c.isActive());
                    return m;
                }).toList();
    }

    private User requireMutableUser(UUID id) {
        User user = requireUser(id);
        if (user.getRole() != UserRole.STUDENT && user.getRole() != UserRole.INSTRUCTOR) {
            throw conflict("User account is immutable");
        }
        return user;
    }

    private User requireUser(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException(id, "User"));
    }

    private void requireManagedRole(UserRole role) {
        if (role != UserRole.STUDENT && role != UserRole.INSTRUCTOR) {
            throw conflict("Only STUDENT and INSTRUCTOR roles are allowed");
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private Map<String, Object> safeUser(User user) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("email", user.getEmail());
        value.put("role", user.getRole());
        value.put("accountStatus", user.getAccountStatus());
        value.put("emailVerified", user.getEmailVerified());
        value.put("firstName", user.getFirstName());
        value.put("lastName", user.getLastName());
        return value;
    }

    private <E extends Enum<E>> EnumMap<E, Long> zeroes(Class<E> type) {
        EnumMap<E, Long> counts = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            counts.put(value, 0L);
        }
        return counts;
    }
}
