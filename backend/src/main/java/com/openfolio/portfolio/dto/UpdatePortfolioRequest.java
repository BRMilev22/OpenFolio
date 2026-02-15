package com.openfolio.portfolio.dto;

import jakarta.validation.constraints.Size;

public record UpdatePortfolioRequest(
        @Size(max = 255) String title,
        @Size(max = 500) String tagline,
        String themeKey,
        Boolean published) {
}
