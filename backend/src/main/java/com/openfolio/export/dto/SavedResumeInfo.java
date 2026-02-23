package com.openfolio.export.dto;

import java.time.LocalDateTime;

public record SavedResumeInfo(
        Long id,
        Long portfolioId,
        String title,
        String templateKey,
        long fileSizeBytes,
        LocalDateTime createdAt,
        String publicUrl
) {}
