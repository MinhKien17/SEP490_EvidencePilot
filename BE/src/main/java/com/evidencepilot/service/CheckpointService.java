package com.evidencepilot.service;

import com.evidencepilot.dto.response.CheckpointDiffResponse;
import com.evidencepilot.dto.response.CheckpointSectionBaselineResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CheckpointService {
    void capture(UUID projectId, String trigger);

    CheckpointDiffResponse getDiff(UUID projectId);

    /**
     * Returns the section content as captured in the newest checkpoint created strictly before
     * {@code before}. {@code before} is typically the requestedAt of the submission
     * being reviewed, so the checkpoint created by that same submission is excluded and the
     * diff compares the submitted state against the previous locked version.
     */
    CheckpointSectionBaselineResponse getLatestSectionBaseline(
            UUID projectId, UUID sectionId, LocalDateTime before);
}
