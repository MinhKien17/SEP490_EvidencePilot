package com.evidencepilot.controller;

import com.evidencepilot.dto.response.CheckpointDiffResponse;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.repository.ProjectRepository;
import com.evidencepilot.service.CheckpointService;
import com.evidencepilot.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Checkpoints", description = "Project checkpoints and diff between review milestones")
public class CheckpointController {

    private final CheckpointService checkpointService;
    private final ProjectRepository projectRepository;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Get diff between the two latest project checkpoints",
            description = "Compares the two most recent checkpoints: claims added/removed/changed, "
                    + "mapping accept/reject deltas, per-section word count deltas and answered feedback delta.")
    @GetMapping("/{projectId}/checkpoints/diff")
    public CheckpointDiffResponse getDiff(@PathVariable UUID projectId) {
        currentUserService.requireProjectAccess(
                currentUserService.requireCurrentUser(),
                projectRepository.findById(projectId)
                        .orElseThrow(() -> new ResourceNotFoundException(projectId, "Project")));
        return checkpointService.getDiff(projectId);
    }
}
