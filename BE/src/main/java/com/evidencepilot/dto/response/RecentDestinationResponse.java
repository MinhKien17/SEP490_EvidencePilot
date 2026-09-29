package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record RecentDestinationResponse(
        String kind,
        UUID refId,
        String tab,
        String label,
        String context,
        String link,
        LocalDateTime lastOpenedAt
) {
}
