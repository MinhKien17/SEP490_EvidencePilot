package com.evidencepilot.service.impl;

import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EmailVerificationServiceImplAccountStatusTest {

    @Test
    void verificationActivatesOnlyPendingAccount() throws Exception {
        UserRepository users = mock(UserRepository.class);
        EmailVerificationServiceImpl service = new EmailVerificationServiceImpl(
                users, null, "https://app.test/verify", Duration.ofHours(1));
        User pending = tokenUser(AccountStatus.PENDING);
        when(users.findByEmailVerificationTokenHash(hash("pending"))).thenReturn(Optional.of(pending));

        service.verifyEmail("pending");

        assertThat(pending.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(pending.getEmailVerified()).isTrue();

        User banned = tokenUser(AccountStatus.BANNED);
        when(users.findByEmailVerificationTokenHash(hash("banned"))).thenReturn(Optional.of(banned));
        service.verifyEmail("banned");
        assertThat(banned.getAccountStatus()).isEqualTo(AccountStatus.BANNED);
    }

    private static User tokenUser(AccountStatus status) {
        User user = new User();
        user.setEmail("student@test.com");
        user.setAccountStatus(status);
        user.setEmailVerified(false);
        user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
        return user;
    }

    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
