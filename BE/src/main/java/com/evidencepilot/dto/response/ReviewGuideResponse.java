package com.evidencepilot.dto.response;

import java.util.List;

public record ReviewGuideResponse(
        String sectionType,
        String guidance,
        List<String> checklist) {
}
