package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.InstructorFeedbackRequest;
import com.evidencepilot.dto.request.SubmitReviewRequest;
import com.evidencepilot.dto.response.FeedbackRequestResponseDto;
import com.evidencepilot.dto.response.InstructorFeedbackResponseDto;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.FeedbackRequest;
import com.evidencepilot.model.FeedbackStatus;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.PaperSection;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.PaperSectionRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import com.evidencepilot.service.FeedbackService;
import com.evidencepilot.service.SystemNotificationService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRequestRepository feedbackRequestRepository;
    private final InstructorFeedbackRepository instructorFeedbackRepository;
    private final PaperSectionRepository paperSectionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final SystemNotificationService systemNotificationService;

    @Override
    public List<FeedbackRequestResponseDto> findAllForCurrentUser() {
        User currentUser = currentUserService.requireCurrentUser();
        List<FeedbackRequest> requests;
        if (currentUserService.isAdmin(currentUser)) {
            requests = feedbackRequestRepository.findAll();
        } else if (currentUserService.isInstructor(currentUser)) {
            requests = feedbackRequestRepository.findByInstructorId(currentUser.getId());
        } else {
            requests = feedbackRequestRepository.findByStudentId(currentUser.getId());
        }
        return requests.stream().map(FeedbackRequestResponseDto::fromEntity).toList();
    }

    @Override
    @Transactional
    public FeedbackRequestResponseDto submitForReview(UUID projectId, SubmitReviewRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectWriteAccess(currentUser, project);
        if (project.getStatus() == ProjectStatus.IN_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project is already in review.");
        }
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only ACTIVE projects can be submitted for review.");
        }

        UUID instructorId = request != null ? request.instructorId() : null;
        User instructor = instructorId == null ? project.getInstructor() : userRepository.findById(instructorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Instructor not found: " + instructorId));
        if (instructor == null || instructor.getRole() != UserRole.INSTRUCTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project has no instructor.");
        }
        User student = project.getStudent();
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project has no student.");
        }

        FeedbackRequest feedbackRequest = new FeedbackRequest();
        feedbackRequest.setProject(project);
        feedbackRequest.setStudent(student);
        feedbackRequest.setInstructor(instructor);
        feedbackRequest.setStatus(FeedbackStatus.PENDING);
        feedbackRequest.setRequestedAt(LocalDateTime.now());

        project.setStatus(ProjectStatus.SUBMITTED_FOR_REVIEW);
        projectRepository.save(project);

        FeedbackRequest saved = feedbackRequestRepository.save(feedbackRequest);
        systemNotificationService.createNotification(
                instructor,
                currentUser,
                "REVIEW_SUBMITTED",
                saved.getId(),
                currentUser.getEmail() + " submitted project \"" + project.getTitle() + "\" for review.");
        return FeedbackRequestResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public InstructorFeedbackResponseDto comment(UUID feedbackRequestId, InstructorFeedbackRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        FeedbackRequest feedbackRequest = requireFeedbackAccess(feedbackRequestId, currentUser, true);
        if (feedbackRequest.getStatus() != FeedbackStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Feedback request closed.");
        }
        if (feedbackRequest.getProject().getStatus() == ProjectStatus.COMPLETED
                || feedbackRequest.getProject().getStatus() == ProjectStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project is read-only.");
        }
        PaperSection section = requireSectionInProject(request.sectionId(), feedbackRequest.getProject());

        InstructorFeedback feedback = new InstructorFeedback();
        feedback.setRequest(feedbackRequest);
        feedback.setSection(section);
        feedback.setInstructor(currentUserService.isAdmin(currentUser)
                ? feedbackRequest.getInstructor()
                : currentUser);
        feedback.setLineReference(request.lineReference());
        feedback.setContent(request.content());
        feedback.setCreatedAt(LocalDateTime.now());
        InstructorFeedback saved = instructorFeedbackRepository.save(feedback);
        systemNotificationService.createNotification(
                feedbackRequest.getStudent(),
                currentUser,
                "INSTRUCTOR_FEEDBACK_ADDED",
                feedbackRequest.getId(),
                currentUser.getEmail() + " added feedback to project \""
                        + feedbackRequest.getProject().getTitle() + "\".");
        return InstructorFeedbackResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public FeedbackRequestResponseDto updateStatus(UUID feedbackRequestId, String status) {
        User currentUser = currentUserService.requireCurrentUser();
        FeedbackStatus newStatus;
        try {
            newStatus = FeedbackStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
        if (newStatus == FeedbackStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
        }
        FeedbackRequest feedbackRequest = transition(feedbackRequestId, newStatus, ProjectStatus.RETURNED, currentUser);
        systemNotificationService.createNotification(
                feedbackRequest.getStudent(),
                currentUser,
                "REVIEW_STATUS_CHANGED",
                feedbackRequest.getId(),
                "Review status for project \"" + feedbackRequest.getProject().getTitle()
                        + "\" changed to " + newStatus + ".");
        return FeedbackRequestResponseDto.fromEntity(feedbackRequest);
    }

    private FeedbackRequest transition(UUID id, FeedbackStatus status, ProjectStatus projectStatus, User currentUser) {
        FeedbackRequest feedbackRequest = requireFeedbackAccess(id, currentUser, true);
        feedbackRequest.setStatus(status);
        feedbackRequest.getProject().setStatus(projectStatus);
        projectRepository.save(feedbackRequest.getProject());
        return feedbackRequestRepository.save(feedbackRequest);
    }

    private FeedbackRequest requireFeedbackAccess(UUID id, User currentUser, boolean instructorOnly) {
        FeedbackRequest feedbackRequest = feedbackRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Feedback request not found: " + id));
        if (currentUserService.isAdmin(currentUser)) {
            return feedbackRequest;
        }
        boolean isInstructor = feedbackRequest.getInstructor() != null
                && currentUser.getId().equals(feedbackRequest.getInstructor().getId());
        boolean isStudent = feedbackRequest.getStudent() != null
                && currentUser.getId().equals(feedbackRequest.getStudent().getId());
        if ((instructorOnly && isInstructor) || (!instructorOnly && (isInstructor || isStudent))) {
            return feedbackRequest;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Feedback access denied.");
    }

    private PaperSection requireSectionInProject(UUID sectionId, Project project) {
        PaperSection section = paperSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paper section not found: " + sectionId));
        Project sectionProject = section.getDocument() != null ? section.getDocument().getProject() : null;
        if (sectionProject == null || !project.getId().equals(sectionProject.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section does not belong to feedback project.");
        }
        return section;
    }
}
