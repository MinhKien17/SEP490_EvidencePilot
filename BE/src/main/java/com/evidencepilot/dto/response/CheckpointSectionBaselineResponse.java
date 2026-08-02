package com.evidencepilot.dto.response;

import java.time.LocalDateTime;

public record CheckpointSectionBaselineResponse(
        String contentTex,
        String trigger,
        LocalDateTime createdAt
) {
}
