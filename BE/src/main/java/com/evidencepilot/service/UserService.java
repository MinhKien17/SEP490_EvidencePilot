package com.evidencepilot.service;

import com.evidencepilot.dto.request.UserProfileUpdateRequest;
import com.evidencepilot.dto.response.UserResponse;
import java.util.UUID;

public interface UserService {
    UserResponse findUserById(UUID id);
    UserResponse updateUserProfile(UUID userId, UserProfileUpdateRequest request);
}
