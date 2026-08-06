package com.evidencepilot.dto.response;

import java.util.List;
import java.util.UUID;

public record SectionAuditResponse(
        UUID sectionId,
        String fingerprint,
        List<SectionAuditFindingResponse> findings) {
}
