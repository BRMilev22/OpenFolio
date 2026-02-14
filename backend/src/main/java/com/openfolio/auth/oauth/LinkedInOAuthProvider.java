package com.openfolio.auth.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component("linkedinOAuthProvider")
public class LinkedInOAuthProvider implements OAuthProvider {

    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String PROFILE_URL = "https://api.linkedin.com/v2/userinfo";

    @Value("${linkedin.client-id}")
    private String clientId;

    @Value("${linkedin.client-secret}")
    private String clientSecret;

    private final RestClient restClient;

    public LinkedInOAuthProvider(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<String, Object> response = restClient.post()
                .uri(TOKEN_URL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(form)
                .retrieve()
                .body(Map.class);
        return response != null ? (String) response.get("access_token") : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthProfile fetchProfile(String accessToken) {
        Map<String, Object> info = restClient.get()
                .uri(PROFILE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (info == null) throw new IllegalStateException("LinkedIn profile is empty");

        return new OAuthProfile(
                (String) info.get("sub"),
                (String) info.get("email"),
                (String) info.get("name"),
                (String) info.get("picture"),
                null);
    }
}
