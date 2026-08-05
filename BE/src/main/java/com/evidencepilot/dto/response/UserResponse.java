package com.evidencepilot.dto.response;

import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.UserRole;
import lombok.Builder;
import lombok.Getter;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private final UUID id;
    private final String email;
    private final UserRole role;
    private final String studentCode;
    private final String firstName;
    private final String lastName;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .studentCode(user.getStudentCode())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }
}
