package com.evidencepilot.dto.request;

import com.evidencepilot.model.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusRequest(@NotNull AccountStatus status) {
}
