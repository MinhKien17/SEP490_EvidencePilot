package com.evidencepilot.dto.response;

import java.util.List;

public record FormatScanResponse(
    String paperTitle,
    List<ScanFinding> findings
) {
    public record ScanFinding(
        String category,
        String severity,
        String section,
        String message,
        String suggestion
    ) {}
}
