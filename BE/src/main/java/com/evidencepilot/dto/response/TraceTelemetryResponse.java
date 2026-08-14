package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TraceTelemetryResponse(
        Overview overview,
        List<SectionMetrics> sections,
        List<RoundMetrics> rounds
) {
    public record Overview(
            long reviewRounds,
            long findings,
            long addressed,
            long unaddressed,
            long pendingInstructor,
            long effective,
            long partial,
            long ineffective,
            double actionRate,
            double effectiveRate,
            long averageTimeToActionMs
    ) {
    }

    public record SectionMetrics(
            UUID sectionId,
            String sectionTitle,
            long reviewRounds,
            long findings,
            long addressed,
            long unaddressed,
            long pendingInstructor,
            long effective,
            long partial,
            long ineffective,
            double actionRate,
            double effectiveRate,
            long averageTimeToActionMs,
            UUID latestRoundId,
            LocalDateTime latestRoundAt,
            Long findingDelta
    ) {
    }

    public record RoundMetrics(
            UUID roundId,
            UUID sectionId,
            String sectionTitle,
            LocalDateTime runAt,
            long findingCount,
            long addressedCount,
            long pendingInstructor,
            long effective,
            long partial,
            long ineffective,
            Long findingDelta
    ) {
    }
}
