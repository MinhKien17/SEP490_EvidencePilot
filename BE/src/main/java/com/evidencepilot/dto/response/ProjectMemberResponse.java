package com.evidencepilot.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectMemberResponse(
    UUID id,
    UUID projectId,
    UUID userId,
    String role,
    LocalDateTime joinedAt,
    String firstName,
    String lastName,
    String email,
    String userRole
) {}
