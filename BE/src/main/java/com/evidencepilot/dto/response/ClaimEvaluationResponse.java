package com.evidencepilot.dto.response;

import java.util.List;

public record ClaimEvaluationResponse(String claim, String evaluation, List<String> contextUsed) {}
