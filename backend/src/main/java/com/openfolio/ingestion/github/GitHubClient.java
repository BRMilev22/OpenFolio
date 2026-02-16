package com.openfolio.ingestion.github;

import com.openfolio.ingestion.github.dto.GitHubRepo;
import com.openfolio.ingestion.github.dto.GitHubUser;
import com.openfolio.shared.exception.ApiException;
import com.openfolio.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class GitHubClient {

    private static final String BASE_URL = "https://api.github.com";
    private final RestClient restClient;

    /** Spring-managed constructor — uses configured server token (or none). */
    @Autowired
    public GitHubClient(RestClient.Builder builder,
                        @Value("${github.token:}") String githubToken) {
        RestClient.Builder b = builder
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        if (githubToken != null && !githubToken.isBlank()) {
            b.defaultHeader("Authorization", "Bearer " + githubToken);
        }
        this.restClient = b.build();
    }

    /** Private constructor for per-user token sessions. */
    private GitHubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Returns a new GitHubClient that authenticates all requests with the given user token.
     * Replaces any previously-configured Authorization header.
     * Returns {@code this} unchanged if the token is blank.
     */
    public GitHubClient withUserToken(String token) {
        if (token == null || token.isBlank()) return this;
        RestClient tokenClient = this.restClient.mutate()
                .defaultHeaders(h -> h.set("Authorization", "Bearer " + token))
                .build();
        return new GitHubClient(tokenClient);
    }

    public GitHubUser fetchUser(String username) {
        try {
            return restClient.get()
                    .uri("/users/{username}", username)
                    .retrieve()
                    .body(GitHubUser.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("GitHub user", username);
        } catch (HttpClientErrorException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GITHUB_API_ERROR",
                    "GitHub API error: " + e.getStatusText());
        }
    }

    public List<GitHubRepo> fetchRepos(String username) {
        try {
            List<GitHubRepo> repos = restClient.get()
                    .uri("/users/{username}/repos?per_page=100&type=public&sort=updated", username)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return repos == null ? List.of() : repos;
        } catch (HttpClientErrorException e) {
            return List.of();
        }
    }

    /**
     * Fetches the user's GitHub profile README (the special {username}/{username} repo).
     * Returns the decoded text content, or null if not found.
     */
    @SuppressWarnings("unchecked")
    public String fetchProfileReadme(String username) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/repos/{u}/{u}/readme", username, username)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !response.containsKey("content")) return null;
            String base64 = ((String) response.get("content")).replaceAll("\\s", "");
            return new String(Base64.getDecoder().decode(base64));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Fetches the language bytes for a single repository.
     * Returns a map like {"Java": 95000, "Python": 12000}.
     */
    public Map<String, Long> fetchRepoLanguages(String owner, String repo) {
        try {
            Map<String, Long> langs = restClient.get()
                    .uri("/repos/{owner}/{repo}/languages", owner, repo)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return langs != null ? langs : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * Fetches the README for a specific repository.
     * Returns decoded text content or null.
     */
    @SuppressWarnings("unchecked")
    public String fetchRepoReadme(String owner, String repo) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/repos/{owner}/{repo}/readme", owner, repo)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !response.containsKey("content")) return null;
            String base64 = ((String) response.get("content")).replaceAll("\\s", "");
            return new String(Base64.getDecoder().decode(base64));
        } catch (Exception e) {
            return null;
        }
    }
}
