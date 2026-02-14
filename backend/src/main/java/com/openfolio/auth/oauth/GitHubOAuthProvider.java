package com.openfolio.auth.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component("githubOAuthProvider")
public class GitHubOAuthProvider implements OAuthProvider {

    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    private final RestClient restClient;

    public GitHubOAuthProvider(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String exchangeCodeForToken(String code, String redirectUri) {
        Map<String, Object> response = restClient.post()
                .uri(TOKEN_URL)
                .header("Accept", "application/json")
                .body(Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "code", code,
                        "redirect_uri", redirectUri != null ? redirectUri : ""))
                .retrieve()
                .body(Map.class);
        return response != null ? (String) response.get("access_token") : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthProfile fetchProfile(String accessToken) {
        Map<String, Object> user = restClient.get()
                .uri(USER_URL)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .body(Map.class);

        if (user == null) throw new IllegalStateException("GitHub user profile is empty");

        return new OAuthProfile(
                String.valueOf(user.get("id")),
                (String) user.get("email"),
                (String) user.get("name"),
                (String) user.get("avatar_url"),
                (String) user.get("login"));
    }
}
