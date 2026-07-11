package com.evidencepilot.service;

import com.evidencepilot.dto.request.UserProfileUpdateRequest;
import com.evidencepilot.mapper.UserMapper;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    private final UserRepository users = mock(UserRepository.class);
    private final UserMapper mapper = mock(UserMapper.class);
    private final UserServiceImpl service = new UserServiceImpl(users, mapper);

    @Test
    void findUserById_mapsExistingUser() {
        UUID id = UUID.randomUUID();
        User user = new User();
        when(users.findById(id)).thenReturn(Optional.of(user));

        service.findUserById(id);

        verify(mapper).toUserResponse(user);
    }

    @Test
    void findUserById_rejectsMissingUser() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.findUserById(id)).hasMessageContaining(id.toString());
    }

    @Test
    void updateUserProfile_updatesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setFirstName("Old");
        user.setLastName("Kept");
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setFirstName("New");
        when(users.findById(id)).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        service.updateUserProfile(id, request);

        org.assertj.core.api.Assertions.assertThat(user.getFirstName()).isEqualTo("New");
        org.assertj.core.api.Assertions.assertThat(user.getLastName()).isEqualTo("Kept");
        verify(mapper).toUserResponse(user);
    }
}
