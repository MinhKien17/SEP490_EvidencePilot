package com.evidencepilot.service;

import com.evidencepilot.domain.entity.Claim;
import com.evidencepilot.domain.entity.Dataset;
import com.evidencepilot.domain.entity.Paper;
import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.Source;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.ProjectStatus;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final FeedbackRequestRepository feedbackRequestRepository;

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String email) || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."));
    }

    public boolean isAdmin(User user) {
        return user.getRole() == UserRole.ADMIN;
    }

    public boolean isInstructor(User user) {
        return user.getRole() == UserRole.INSTRUCTOR;
    }

    public boolean ownsUserIdOrAdmin(User currentUser, Integer userId) {
        return isAdmin(currentUser) || currentUser.getId().equals(userId);
    }

    public void requireRole(User currentUser, UserRole role) {
        if (!isAdmin(currentUser) && currentUser.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role.");
        }
    }

    public void requireUserIdOrAdmin(User currentUser, Integer userId) {
        if (!ownsUserIdOrAdmin(currentUser, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-user access denied.");
        }
    }

    public void requireProjectAccess(User currentUser, Project project) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (project.getStudent() == null || !currentUser.getId().equals(project.getStudent().getId())) {
            if (currentUser.getRole() == UserRole.INSTRUCTOR
                    && project.getStatus() == ProjectStatus.IN_REVIEW
                    && feedbackRequestRepository.existsByProjectIdAndInstructorId(project.getId(), currentUser.getId())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Project access denied.");
        }
    }

    public void requireProjectWriteAccess(User currentUser, Project project) {
        requireProjectAccess(currentUser, project);
        if (isAdmin(currentUser)) {
            return;
        }
        if (project.getStatus() == ProjectStatus.IN_REVIEW) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Project is in review and cannot be modified.");
        }
        if (project.getStudent() == null || !currentUser.getId().equals(project.getStudent().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Project write access denied.");
        }
    }

    public void requireDatasetAccess(User currentUser, Dataset dataset) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (dataset.getInstructor() == null || !currentUser.getId().equals(dataset.getInstructor().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Dataset access denied.");
        }
    }

    public void requireClaimAccess(User currentUser, Claim claim) {
        requireProjectAccess(currentUser, claim.getProject());
    }

    public void requirePaperAccess(User currentUser, Paper paper) {
        requireProjectAccess(currentUser, paper.getProject());
    }

    public void requireSourceAccess(User currentUser, Source source) {
        if (isAdmin(currentUser)) {
            return;
        }
        if (source.getProject() != null) {
            requireProjectAccess(currentUser, source.getProject());
            return;
        }
        if (source.getDataset() != null) {
            requireDatasetAccess(currentUser, source.getDataset());
            return;
        }
        if (source.getUploadedBy() != null && currentUser.getId().equals(source.getUploadedBy().getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Source access denied.");
    }
}
