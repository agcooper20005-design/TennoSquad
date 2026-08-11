package com.aces.tennosquad.dto.user;

import com.aces.tennosquad.model.enums.UserRole;

public record MeResponse(Long id, String userName, String warframeUserName, UserRole role) {

}
