package com.openfolio.ingestion.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepo(
        Long id,
        String name,
        @JsonProperty("full_name") String fullName,
        String description,
        @JsonProperty("html_url") String htmlUrl,
        String language,
        @JsonProperty("stargazers_count") int stars,
        @JsonProperty("forks_count") int forks,
        boolean fork,
        boolean archived) {
}
