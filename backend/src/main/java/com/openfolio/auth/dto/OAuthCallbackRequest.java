package com.openfolio.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthCallbackRequest(
        @NotBlank String code,
        String redirectUri) {
}
