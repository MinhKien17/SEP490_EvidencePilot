package com.evidencepilot.controller;

import com.evidencepilot.dto.request.InstructorFeedbackRequest;
import com.evidencepilot.dto.request.SubmitReviewRequest;
import com.evidencepilot.dto.response.FeedbackRequestResponseDto;
import com.evidencepilot.dto.response.InstructorFeedbackResponseDto;
import com.evidencepilot.model.FeedbackRequest;
import com.evidencepilot.model.FeedbackStatus;
import com.evidencepilot.model.InstructorFeedback;
import com.evidencepilot.model.Project;
import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.ProjectStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.FeedbackRequestRepository;
import com.evidencepilot.repository.InstructorFeedbackRepository;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Feedback request lifecycle and instructor review")
public class FeedbackController {

    private final FeedbackRequestRepository feedbackRequestRepository;
    private final InstructorFeedbackRepository instructorFeedbackRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Operation(summary = "List feedback requests",
            description = "Returns all feedback requests scoped to the current user. "
                    + "Admins see all; instructors see requests assigned to them; students see their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback request list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/feedback-requests")
    public List<FeedbackRequestResponseDto> findAll() {
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

    @Operation(summary = "Submit project for review",
            description = "Creates a PENDING feedback request for the specified instructor. "
                    + "Sets the project status to IN_REVIEW. The current user is extracted from the JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feedback request created"),
            @ApiResponse(responseCode = "400", description = "Assigned user is not an instructor"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Project or instructor not found")
    })
    @PostMapping("/projects/{projectId}/reviews")
    @Transactional
    public ResponseEntity<FeedbackRequestResponseDto> submitForReview(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId,
            @Valid @RequestBody SubmitReviewRequest request) {

        User currentUser = currentUserService.requireCurrentUser();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Project not found: " + projectId));
        currentUserService.requireProjectWriteAccess(currentUser, project);

        User instructor = userRepository.findById(request.instructorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Instructor not found: " + request.instructorId()));
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
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackRequestResponseDto.fromEntity(saved));
    }

    @Operation(summary = "Submit instructor feedback",
            description = "Creates or updates instructor feedback for a feedback request. "
                    + "The current user is extracted from the JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback saved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Not the assigned instructor"),
            @ApiResponse(responseCode = "404", description = "Feedback request not found")
    })
    @PostMapping("/feedback-requests/{id}/feedback")
    @Transactional
    public InstructorFeedbackResponseDto comment(
            @Parameter(description = "Feedback request UUID") @PathVariable UUID id,
            @Valid @RequestBody InstructorFeedbackRequest request) {

        User currentUser = currentUserService.requireCurrentUser();
        FeedbackRequest feedbackRequest = requireFeedbackAccess(id, currentUser, true);

        InstructorFeedback feedback = instructorFeedbackRepository.findByRequestId(id)
                .orElseGet(InstructorFeedback::new);
        feedback.setRequest(feedbackRequest);
        feedback.setInstructor(currentUserService.isAdmin(currentUser)
                ? feedbackRequest.getInstructor()
                : currentUser);
        feedback.setContent(request.content());
        feedback.setCreatedAt(LocalDateTime.now());
        InstructorFeedback saved = instructorFeedbackRepository.save(feedback);
        return InstructorFeedbackResponseDto.fromEntity(saved);
    }

    @Operation(summary = "Update feedback request status",
            description = "Transitions a feedback request to a new status (RETURNED, REVIEWED, or REJECTED) "
                    + "and sets the project status to ACTIVE. Replaces the old RPC-style status endpoints.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Not the assigned instructor"),
            @ApiResponse(responseCode = "404", description = "Feedback request not found")
    })
    @PatchMapping("/feedback-requests/{id}/status")
    @Transactional
    public FeedbackRequestResponseDto updateStatus(
            @Parameter(description = "Feedback request UUID") @PathVariable UUID id,
            @Parameter(description = "New status: RETURNED, REVIEWED, or REJECTED") @RequestParam FeedbackStatus status) {
        return FeedbackRequestResponseDto.fromEntity(transition(id, status, ProjectStatus.ACTIVE));
    }

    private FeedbackRequest transition(UUID id, FeedbackStatus status, ProjectStatus projectStatus) {
        User currentUser = currentUserService.requireCurrentUser();
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
}
