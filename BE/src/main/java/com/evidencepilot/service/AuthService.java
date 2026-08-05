package com.evidencepilot.service;

import com.evidencepilot.dto.request.LoginRequest;
import com.evidencepilot.dto.request.UpdatePasswordRequest;
import com.evidencepilot.dto.response.AuthResponse;

import java.util.UUID;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String token);
    void updatePassword(UUID userId, UpdatePasswordRequest request);
}
