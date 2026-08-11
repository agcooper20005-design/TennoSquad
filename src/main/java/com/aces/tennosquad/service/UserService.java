package com.aces.tennosquad.service;

import com.aces.tennosquad.dto.user.CreateUserRequest;
import com.aces.tennosquad.dto.user.UserResponse;
import com.aces.tennosquad.exception.DuplicateResourceException;
import com.aces.tennosquad.exception.ResourceNotFoundException;
import com.aces.tennosquad.model.User;
import com.aces.tennosquad.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        String normalizedUserName = request.userName().trim();
        String normalizedWarframeUserName =
                request.warframeUserName().trim();

        if (userRepository.existsByUserNameIgnoreCase(normalizedUserName)) {
            throw new DuplicateResourceException(
                    "Username already exists: " + normalizedUserName
            );
        }

        if (userRepository.existsByWarframeUserNameIgnoreCase(
                normalizedWarframeUserName
        )) {
            throw new DuplicateResourceException(
                    "Warframe username already exists: "
                            + normalizedWarframeUserName
            );
        }

        User user = new User();
        user.setUserName(normalizedUserName);
        user.setWarframeUserName(normalizedWarframeUserName);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return mapToResponse(findUserById(userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUserName(String userName) {

        String normalizedUserName = userName.trim();

        User user = userRepository
                .findByUserNameIgnoreCase(normalizedUserName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: "
                                + normalizedUserName
                ));

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByWarframeUserName(
            String warframeUserName
    ) {
        String normalizedWarframeUserName =
                warframeUserName.trim();

        User user = userRepository
                .findByWarframeUserNameIgnoreCase(
                        normalizedWarframeUserName
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with Warframe username: "
                                + normalizedWarframeUserName
                ));

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                ));
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUserName(),
                user.getWarframeUserName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}