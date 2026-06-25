package com.evidencepilot.service;

import com.evidencepilot.domain.entity.User;
import com.evidencepilot.domain.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailVerificationService service =
            new EmailVerificationService(userRepository, null, "http://localhost:5173/verify-email", Duration.ofHours(24));

    @Test
    void createVerificationTokenStoresHashAndExpiryWithoutVerifyingUser() {
        User user = new User();
        user.setEmail("student@example.com");
        user.setRole(UserRole.STUDENT);

        String rawToken = service.createVerificationToken(user);

        assertThat(rawToken).isNotBlank();
        assertThat(user.getEmailVerified()).isFalse();
        assertThat(user.getEmailVerificationTokenHash()).isNotBlank();
        assertThat(user.getEmailVerificationTokenHash()).isNotEqualTo(rawToken);
        assertThat(user.getEmailVerificationTokenExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void createVerificationTokenRequiresConfiguredVerificationUrl() {
        EmailVerificationService missingUrlService =
                new EmailVerificationService(userRepository, null, "", Duration.ofHours(24));
        User user = new User();
        user.setEmail("student@example.com");

        assertThatThrownBy(() -> missingUrlService.createVerificationToken(user))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void verifyEmailMarksUserVerifiedAndClearsToken() {
        User user = new User();
        user.setEmail("student@example.com");
        String rawToken = service.createVerificationToken(user);
        String tokenHash = user.getEmailVerificationTokenHash();
        user.setEmailVerified(false);

        when(userRepository.findByEmailVerificationTokenHash(tokenHash)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String email = service.verifyEmail(rawToken);

        assertThat(email).isEqualTo("student@example.com");
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationTokenHash()).isNull();
        assertThat(user.getEmailVerificationTokenExpiresAt()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailRejectsExpiredToken() {
        User user = new User();
        user.setEmail("student@example.com");
        String rawToken = service.createVerificationToken(user);
        String tokenHash = user.getEmailVerificationTokenHash();
        user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmailVerificationTokenHash(tokenHash)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.verifyEmail(rawToken))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
