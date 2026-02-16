package com.openfolio.ingestion.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubUser(
        Long id,
        String login,
        String name,
        String bio,
        @JsonProperty("avatar_url") String avatarUrl,
        String location,
        String blog,
        @JsonProperty("public_repos") int publicRepos,
        int followers) {
}
