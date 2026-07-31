package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CheckpointDiffResponse(
    UUID projectId,
    LocalDateTime from,
    LocalDateTime to,
    String fromTrigger,
    String toTrigger,
    List<ClaimChange> claimsAdded,
    List<ClaimChange> claimsRemoved,
    List<ClaimChange> claimsChanged,
    int mappingsAcceptedDelta,
    int mappingsRejectedDelta,
    int feedbackAnsweredDelta,
    List<WordCountDelta> sectionWordDeltas
) {
    public record ClaimChange(UUID id, Integer version, String contentHash) {}

    public record WordCountDelta(UUID sectionId, int fromWords, int toWords) {}
}
