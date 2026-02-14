package com.openfolio.auth.oauth;

public record OAuthProfile(
        String providerUid,
        String email,
        String displayName,
        String avatarUrl,
        String login) {
}
