package com.openfolio.portfolio.dto;

import com.openfolio.portfolio.Portfolio;

import java.time.LocalDateTime;

public record PortfolioSummaryResponse(
        Long id,
        String slug,
        String title,
        String tagline,
        boolean published,
        String themeKey,
        long projectCount,
        long skillCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PortfolioSummaryResponse from(Portfolio p, long projectCount, long skillCount) {
        return new PortfolioSummaryResponse(
                p.getId(),
                p.getSlug(),
                p.getTitle(),
                p.getTagline(),
                p.isPublished(),
                p.getThemeKey(),
                projectCount,
                skillCount,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
