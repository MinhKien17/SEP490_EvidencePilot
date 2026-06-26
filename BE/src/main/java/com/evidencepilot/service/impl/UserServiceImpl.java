package com.evidencepilot.service.impl;

import com.evidencepilot.dto.request.UserProfileUpdateRequest;
import com.evidencepilot.exception.ResourceNotFoundException;
import com.evidencepilot.model.User;
import com.evidencepilot.repository.UserRepository;
import com.evidencepilot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id, "User"));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));
    }

    @Override
    public User updateProfile(UUID userId, UserProfileUpdateRequest request) {
        User user = findById(userId);
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        return userRepository.save(user);
    }
}
