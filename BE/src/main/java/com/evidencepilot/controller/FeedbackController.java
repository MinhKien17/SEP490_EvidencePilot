package com.evidencepilot.controller;

import com.evidencepilot.domain.entity.FeedbackRequest;
import com.evidencepilot.domain.entity.InstructorFeedback;
import com.evidencepilot.domain.entity.Project;
import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.FeedbackStatus;
import com.evidencepilot.domain.enums.ProjectStatus;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.dto.InstructorFeedbackRequest;
import com.evidencepilot.dto.SubmitReviewRequest;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRequestRepository feedbackRequestRepository;
    private final InstructorFeedbackRepository instructorFeedbackRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/feedback-requests")
    public List<FeedbackRequest> findAll() {
        User currentUser = currentUserService.requireCurrentUser();
        if (currentUserService.isAdmin(currentUser)) {
            return feedbackRequestRepository.findAll();
        }
        if (currentUserService.isInstructor(currentUser)) {
            return feedbackRequestRepository.findByInstructorId(currentUser.getId());
        }
        return feedbackRequestRepository.findByStudentId(currentUser.getId());
    }

    @PostMapping("/projects/{projectId}/submit-review")
    @Transactional
    public ResponseEntity<FeedbackRequest> submitForReview(
            @PathVariable Integer projectId,
            @Valid @RequestBody SubmitReviewRequest request) {

        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectWriteAccess(currentUser, project);

        User instructor = userRepository.findById(request.getInstructorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Instructor not found: " + request.getInstructorId()));
        if (instructor.getRole() != UserRole.INSTRUCTOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user is not an instructor.");
        }

        FeedbackRequest feedbackRequest = new FeedbackRequest();
        feedbackRequest.setProject(project);
        feedbackRequest.setStudent(project.getStudent());
        feedbackRequest.setInstructor(instructor);
        feedbackRequest.setStatus(FeedbackStatus.PENDING);
        feedbackRequest.setRequestedAt(LocalDateTime.now());

        project.setStatus(ProjectStatus.IN_REVIEW);
        projectRepository.save(project);

        FeedbackRequest saved = feedbackRequestRepository.save(feedbackRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/feedback-requests/{id}/feedback")
    @Transactional
    public InstructorFeedback comment(
            @PathVariable Integer id,
            @Valid @RequestBody InstructorFeedbackRequest request) {

        User currentUser = currentUserService.requireCurrentUser();
        FeedbackRequest feedbackRequest = requireFeedbackAccess(id, currentUser, true);

        InstructorFeedback feedback = instructorFeedbackRepository.findByRequestId(id)
                .orElseGet(InstructorFeedback::new);
        feedback.setRequest(feedbackRequest);
        feedback.setInstructor(currentUserService.isAdmin(currentUser)
                ? feedbackRequest.getInstructor()
                : currentUser);
        feedback.setContent(request.getContent());
        feedback.setCreatedAt(LocalDateTime.now());
        return instructorFeedbackRepository.save(feedback);
    }

    @PostMapping("/feedback-requests/{id}/return-to-active")
    @Transactional
    public FeedbackRequest returnToActive(@PathVariable Integer id) {
        return transition(id, FeedbackStatus.RETURNED, ProjectStatus.ACTIVE);
    }

    @PostMapping("/feedback-requests/{id}/reviewed")
    @Transactional
    public FeedbackRequest markReviewed(@PathVariable Integer id) {
        return transition(id, FeedbackStatus.REVIEWED, ProjectStatus.ACTIVE);
    }

    @PostMapping("/feedback-requests/{id}/rejected")
    @Transactional
    public FeedbackRequest markRejected(@PathVariable Integer id) {
        return transition(id, FeedbackStatus.REJECTED, ProjectStatus.ACTIVE);
    }

    private FeedbackRequest transition(Integer id, FeedbackStatus status, ProjectStatus projectStatus) {
        User currentUser = currentUserService.requireCurrentUser();
        FeedbackRequest feedbackRequest = requireFeedbackAccess(id, currentUser, true);
        feedbackRequest.setStatus(status);
        feedbackRequest.getProject().setStatus(projectStatus);
        projectRepository.save(feedbackRequest.getProject());
        return feedbackRequestRepository.save(feedbackRequest);
    }

    private FeedbackRequest requireFeedbackAccess(Integer id, User currentUser, boolean instructorOnly) {
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
}
