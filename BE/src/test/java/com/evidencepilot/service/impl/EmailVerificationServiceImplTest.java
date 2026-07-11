package com.evidencepilot.service.impl;

import com.evidencepilot.model.User;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailVerificationServiceImplTest {

    private final UserRepository users = mock(UserRepository.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void createVerificationToken_setsHashExpiryAndUnverifiedState() {
        User user = user();
        var service = service(mailSender, "https://app.test/verify");

        String token = service.createVerificationToken(user);

        assertThat(token).isNotBlank();
        assertThat(user.getEmailVerified()).isFalse();
        assertThat(user.getEmailVerificationTokenHash()).hasSize(64).doesNotContain(token);
        assertThat(user.getEmailVerificationTokenExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void createVerificationToken_requiresConfiguredUrl() {
        var service = service(mailSender, " ");
        assertThatThrownBy(() -> service.createVerificationToken(user()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void sendVerificationEmail_sendsConfiguredLink() {
        User user = user();
        var service = service(mailSender, "https://app.test/verify");

        service.sendVerificationEmail(user, "raw-token");

        verify(mailSender).send(argThat((SimpleMailMessage message) ->
                message.getText().contains("https://app.test/verify?token=raw-token")));
    }

    @Test
    void verifyEmail_marksUserVerifiedAndClearsToken() {
        var service = service(mailSender, "https://app.test/verify");
        User user = user();
        String token = service.createVerificationToken(user);
        when(users.findByEmailVerificationTokenHash(user.getEmailVerificationTokenHash()))
                .thenReturn(Optional.of(user));

        assertThat(service.verifyEmail(token)).isEqualTo(user.getEmail());
        assertThat(user.getEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationTokenHash()).isNull();
        assertThat(user.getEmailVerificationTokenExpiresAt()).isNull();
        verify(users).save(user);
    }

    @Test
    void verifyEmail_rejectsBlankInvalidAndExpiredTokens() {
        var service = service(mailSender, "https://app.test/verify");
        assertThatThrownBy(() -> service.verifyEmail(" ")).hasMessageContaining("required");
        assertThatThrownBy(() -> service.verifyEmail("unknown")).hasMessageContaining("Invalid");

        User expired = user();
        expired.setEmailVerificationTokenExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(users.findByEmailVerificationTokenHash(any())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.verifyEmail("expired")).hasMessageContaining("expired");
    }

    private EmailVerificationServiceImpl service(JavaMailSender sender, String url) {
        return new EmailVerificationServiceImpl(users, sender, url, Duration.ofHours(24));
    }

    private static User user() {
        User user = new User();
        user.setEmail("user@test.com");
        return user;
    }
}
