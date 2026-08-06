package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.SectionAuditFindingStatus;
import jakarta.validation.constraints.NotNull;

public record SectionAuditStatusUpdateRequest(
        @NotNull SectionAuditFindingStatus status) {
}
