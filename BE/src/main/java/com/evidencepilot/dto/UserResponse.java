package com.evidencepilot.dto;

import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Integer id,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
