package com.evidencepilot.dto.response;

import com.evidencepilot.model.enums.PaperStandard;

import java.util.List;

public record PaperStandardSuggestionResponse(
        PaperStandard suggestedStandard,
        int confidencePercent,
        List<String> evidence) {
}
