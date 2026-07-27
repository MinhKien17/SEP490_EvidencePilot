package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.PaperStandard;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProjectCreateRequest(
    @NotBlank String title,
    String description,
    PaperStandard targetStandard
) {}
