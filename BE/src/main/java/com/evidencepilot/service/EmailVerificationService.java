package com.evidencepilot.service;

import com.evidencepilot.model.User;

public interface EmailVerificationService {

    String createVerificationToken(User user);

    void sendVerificationEmail(User user, String rawToken);

    String verifyEmail(String rawToken);
}
