package com.evidencepilot.service;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;

import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public interface CurrentUserService {

    User requireCurrentUser();

    boolean isAdmin(User user);

    boolean isInstructor(User user);

    boolean ownsUserIdOrAdmin(User currentUser, UUID userId);

    void requireRole(User currentUser, UserRole role);

    void requireUserIdOrAdmin(User currentUser, UUID userId);

    void requireProjectAccess(User currentUser, Project project);

    void requireProjectWriteAccess(User currentUser, Project project);

    void requireProjectManageAccess(User currentUser, Project project);

    void requireCollectionAccess(User currentUser, com.evidencepilot.model.Collection collection);

    void requireClaimAccess(User currentUser, Claim claim);
}
