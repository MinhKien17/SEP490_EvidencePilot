package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.PaperStandard;

import java.util.List;

public record PaperValidationResponse(
    boolean valid,
    List<String> missingSections,
    List<String> extraSections,
    List<String> outOfOrder,
    PaperStandard standardUsed
) {}