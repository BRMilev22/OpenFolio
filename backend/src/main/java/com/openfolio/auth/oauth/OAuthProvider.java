package com.openfolio.auth.oauth;

public interface OAuthProvider {

    String exchangeCodeForToken(String code, String redirectUri);

    OAuthProfile fetchProfile(String accessToken);
}
