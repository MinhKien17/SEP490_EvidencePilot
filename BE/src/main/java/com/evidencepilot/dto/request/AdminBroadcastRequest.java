package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;

public record AdminBroadcastRequest(@NotBlank String message, UserRole role) {
}
