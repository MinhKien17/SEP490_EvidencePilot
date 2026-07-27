package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.PaperStandard;
import jakarta.validation.constraints.NotBlank;

public record ProjectUpdateRequest(
    @NotBlank String title,
    String description,
    PaperStandard targetStandard
) {}
