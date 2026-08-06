package com.evidencepilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SectionAuditFlags(
        @JsonProperty("requires_citation_check") boolean requiresCitationCheck) {
}
