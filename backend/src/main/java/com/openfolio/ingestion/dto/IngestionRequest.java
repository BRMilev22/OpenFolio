package com.openfolio.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record IngestionRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,37}[a-zA-Z0-9])?$",
                message = "Invalid GitHub username format")
        String githubUsername) {
}
