package com.evidencepilot.service;

import com.evidencepilot.domain.entity.User;
import com.evidencepilot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final String verificationUrl;
    private final Duration tokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            UserRepository userRepository,
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.email-verification.url:http://localhost:5173/verify-email}") String verificationUrl,
            @Value("${app.email-verification.token-ttl-hours:24}") long tokenTtlHours) {
        this(userRepository, mailSenderProvider.getIfAvailable(), verificationUrl, Duration.ofHours(tokenTtlHours));
    }

    EmailVerificationService(
            UserRepository userRepository,
            JavaMailSender mailSender,
            String verificationUrl,
            Duration tokenTtl) {
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.verificationUrl = verificationUrl;
        this.tokenTtl = tokenTtl;
    }

    public String createVerificationToken(User user) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        user.setEmailVerified(false);
        user.setEmailVerificationTokenHash(hash(rawToken));
        user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plus(tokenTtl));
        return rawToken;
    }

    public void sendVerificationEmail(User user, String rawToken) {
        String link = verificationUrl + "?token=" + rawToken;
        if (mailSender == null) {
            log.info("Email verification link for {}: {}", user.getEmail(), link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify your Evidence Pilot account");
        message.setText("Verify your Evidence Pilot account by opening this link:\n\n" + link);

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            log.warn("Failed to send verification email to {}", user.getEmail(), exception);
        }
    }

    public String verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token is required");
        }

        User user = userRepository.findByEmailVerificationTokenHash(hash(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid verification token"));

        LocalDateTime expiresAt = user.getEmailVerificationTokenExpiresAt();
        if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiresAt(null);
        userRepository.save(user);
        return user.getEmail();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
