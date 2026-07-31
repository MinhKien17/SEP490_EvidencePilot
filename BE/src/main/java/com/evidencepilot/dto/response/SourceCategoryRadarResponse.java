package com.evidencepilot.dto.response;

import java.util.List;
import java.util.UUID;

public record SourceCategoryRadarResponse(
        String seriesLabel,
        int totalSources,
        List<Axis> axes
) {
    public record Axis(
            UUID categoryId,
            String code,
            String label,
            int sourceCount,
            double percentage
    ) {}
}
