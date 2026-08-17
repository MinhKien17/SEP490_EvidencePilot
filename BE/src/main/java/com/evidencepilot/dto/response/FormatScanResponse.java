package com.evidencepilot.dto.response;

import java.util.List;
import java.util.Map;

public record FormatScanResponse(
    String paperTitle,
    List<ScanFinding> findings,
    Map<String, Integer> citationNumbers,
    List<CitationReference> references
) {
    public record ScanFinding(
        String category,
        String severity,
        String section,
        String message,
        String suggestion
    ) {}

    public record CitationReference(
        String key,
        int number,
        String reference
    ) {}
}
