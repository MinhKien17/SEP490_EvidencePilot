package com.evidencepilot.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthResponse {
    private final String token;
    private final UserResponse user;
    private final boolean passwordChangeNotice;
}
