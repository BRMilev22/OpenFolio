package com.openfolio.user.dto;

public record UpdateUserRequest(
        String displayName,
        String avatarUrl) {
}
