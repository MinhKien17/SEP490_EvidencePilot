package com.evidencepilot.controller;

import com.evidencepilot.dto.request.InstructorFeedbackRequest;
import com.evidencepilot.dto.request.SubmitReviewRequest;
import com.evidencepilot.dto.response.FeedbackRequestResponseDto;
import com.evidencepilot.dto.response.InstructorFeedbackResponseDto;
import com.evidencepilot.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Feedback request lifecycle and instructor review")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "List feedback requests",
            description = "Returns all feedback requests scoped to the current user. "
                    + "Admins see all; instructors see requests assigned to them; students see their own.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback request list returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping("/feedback-requests")
    public List<FeedbackRequestResponseDto> findAll() {
        return feedbackService.findAllForCurrentUser();
    }

    @Operation(summary = "Submit project for review",
            description = "Creates a PENDING feedback request for the specified instructor. "
                    + "Sets the project status to IN_REVIEW. The current user is extracted from the JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Feedback request created"),
            @ApiResponse(responseCode = "400", description = "Project has no instructor or student"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "Project or instructor not found")
    })
    @PostMapping("/projects/{projectId}/reviews")
    public ResponseEntity<FeedbackRequestResponseDto> submitForReview(
            @Parameter(description = "Project UUID") @PathVariable UUID projectId,
            @Valid @RequestBody(required = false) SubmitReviewRequest request) {
        FeedbackRequestResponseDto response = feedbackService.submitForReview(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Submit instructor feedback",
            description = "Creates instructor feedback for one paper section in a feedback request. "
                    + "The current user is extracted from the JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Feedback saved"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Not the assigned instructor"),
            @ApiResponse(responseCode = "404", description = "Feedback request not found")
    })
    @PostMapping("/feedback-requests/{id}/feedback")
    public InstructorFeedbackResponseDto comment(
            @Parameter(description = "Feedback request UUID") @PathVariable UUID id,
            @Valid @RequestBody InstructorFeedbackRequest request) {
        return feedbackService.comment(id, request);
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
    public FeedbackRequestResponseDto updateStatus(
            @Parameter(description = "Feedback request UUID") @PathVariable UUID id,
            @Parameter(description = "New status: RETURNED, REVIEWED, or REJECTED") @RequestParam String status) {
        return feedbackService.updateStatus(id, status);
    }
}
