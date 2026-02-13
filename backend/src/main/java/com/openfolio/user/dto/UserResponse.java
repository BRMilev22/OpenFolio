package com.openfolio.user.dto;

import com.openfolio.user.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        String avatarUrl,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getCreatedAt());
    }
}
