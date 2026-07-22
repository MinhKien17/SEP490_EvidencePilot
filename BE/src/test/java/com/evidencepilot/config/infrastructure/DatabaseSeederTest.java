package com.evidencepilot.config.infrastructure;

import com.evidencepilot.model.User;
import com.evidencepilot.model.enums.AccountStatus;
import com.evidencepilot.model.enums.UserRole;
import com.evidencepilot.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void seedsThreeActiveAccountsFromConfiguration() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));

        new DatabaseSeeder(
                userRepository,
                passwordEncoder,
                "configured-admin@example.com",
                "admin-password",
                "configured-student@example.com",
                "student-password",
                "configured-instructor@example.com",
                "instructor-password")
                .run();

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.times(3)).save(users.capture());

        assertThat(users.getAllValues())
                .extracting(User::getEmail, User::getRole, User::getPasswordHash, User::getAccountStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "configured-admin@example.com", UserRole.ADMIN,
                                "encoded:admin-password", AccountStatus.ACTIVE),
                        org.assertj.core.groups.Tuple.tuple(
                                "configured-student@example.com", UserRole.STUDENT,
                                "encoded:student-password", AccountStatus.ACTIVE),
                        org.assertj.core.groups.Tuple.tuple(
                                "configured-instructor@example.com", UserRole.INSTRUCTOR,
                                "encoded:instructor-password", AccountStatus.ACTIVE));
    }

    @Test
    void skipsSeedingWhenCredentialsAreMissing() {
        new DatabaseSeeder(
                userRepository,
                passwordEncoder,
                "",
                "",
                "",
                "",
                "",
                "")
                .run();

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void configuredSeedUserIsActive() {
        User existing = new User();
        existing.setAccountStatus(AccountStatus.BANNED);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        new DatabaseSeeder(
                userRepository,
                passwordEncoder,
                "",
                "",
                "student@example.com",
                "password",
                "",
                "")
                .run();

        verify(userRepository).save(existing);
        assertThat(existing.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
