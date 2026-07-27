package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record AdminUserRoleRequest(@NotNull UserRole role) {
}
