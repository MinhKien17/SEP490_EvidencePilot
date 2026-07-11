package com.evidencepilot.service.impl;

import com.evidencepilot.model.Claim;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.ProjectMember;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectRole;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;
    private final FeedbackRequestRepository feedbackRequestRepository;

    @Override
    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "No authenticated user found");
        }
        if (auth.getPrincipal() instanceof User user) {
            return user;
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "User not found: " + email));
    }

    @Override
    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    @Override
    public boolean isInstructor(User user) {
        return user.getRole() == UserRole.INSTRUCTOR;
    }

    @Override
    public boolean ownsUserIdOrAdmin(User currentUser, UUID userId) {
        return isAdmin(currentUser) || currentUser.getId().equals(userId);
    }

    @Override
    public void requireRole(User currentUser, UserRole role) {
        if (currentUser.getRole() != role && currentUser.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Requires role: " + role);
        }
    }

    @Override
    public void requireUserIdOrAdmin(User currentUser, UUID userId) {
        if (!ownsUserIdOrAdmin(currentUser, userId)) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Access denied: not your resource");
        }
    }

    @Override
    public void requireProjectAccess(User currentUser, Project project) {
        if (isAdmin(currentUser))
            return;
        if (isInstructor(currentUser)) {
            if (project.getStatus() == ProjectStatus.SUBMITTED_FOR_REVIEW && project.getId() != null
                    && feedbackRequestRepository.existsByProjectIdAndInstructorId(
                            project.getId(), currentUser.getId())) {
                return;
            }
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Instructor access denied to project");
        }
        if (!isProjectMember(currentUser, project)) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Project access denied");
        }
    }

    @Override
    public void requireProjectWriteAccess(User currentUser, Project project) {
        if (isAdmin(currentUser))
            return;
        if (!hasProjectRole(currentUser, project, Set.of(
                ProjectRole.OWNER, ProjectRole.EDITOR, ProjectRole.INSTRUCTOR))) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Write access denied to project");
        }
    }

    @Override
    public void requireProjectManageAccess(User currentUser, Project project) {
        if (isAdmin(currentUser))
            return;
        if (!hasProjectRole(currentUser, project, Set.of(ProjectRole.INSTRUCTOR, ProjectRole.OWNER))) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Project management access denied");
        }
    }

    private boolean isProjectMember(User currentUser, Project project) {
        if (project.getProjectMembers() == null) {
            return false;
        }
        return project.getProjectMembers().stream()
                .map(ProjectMember::getUser)
                .anyMatch(user -> user != null && currentUser.getId().equals(user.getId()));
    }

    private boolean hasProjectRole(User currentUser, Project project, Set<ProjectRole> roles) {
        if (project.getProjectMembers() == null) {
            return false;
        }
        if (project.getStatus() == ProjectStatus.SUBMITTED_FOR_REVIEW ||
            project.getStatus() == ProjectStatus.APPROVED) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Write access denied: project is locked");
        }
        return project.getProjectMembers().stream()
                .anyMatch(pm -> pm.getUser() != null
                        && currentUser.getId().equals(pm.getUser().getId())
                        && roles.contains(pm.getRole()));
    }

    @Override
    public void requireCollectionAccess(User currentUser, com.evidencepilot.model.Collection collection) {
        if (isAdmin(currentUser))
            return;
        if (isInstructor(currentUser)) {
            if (!collection.getInstructor().getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Instructor access denied to collection");
            }
            return;
        }
        throw new ResponseStatusException(
                org.springframework.http.HttpStatus.FORBIDDEN,
                "Students cannot access collections");
    }

    @Override
    public void requireClaimAccess(User currentUser, Claim claim) {
        requireProjectAccess(currentUser, claim.getProject());
    }
}
