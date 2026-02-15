package com.openfolio.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePortfolioRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 500) String tagline) {
}
