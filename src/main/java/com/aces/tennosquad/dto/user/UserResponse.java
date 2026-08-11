package com.aces.tennosquad.dto.user;

import com.aces.tennosquad.model.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String userName,
        String warframeUserName,
        UserRole role,
        LocalDateTime createdAt
) {
}