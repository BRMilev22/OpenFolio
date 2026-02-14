package com.openfolio.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long userId,
        String email,
        String displayName,
        String githubUsername) {
}
