package com.openfolio.publish.dto;

import java.time.LocalDateTime;

public record PublishResponse(
        Long portfolioId,
        String slug,
        String publicUrl,
        int version,
        LocalDateTime publishedAt
) {}
