package com.evidencepilot.dto.request;

import java.util.UUID;

public record SubmitReviewRequest(
    UUID instructorId
) {}
