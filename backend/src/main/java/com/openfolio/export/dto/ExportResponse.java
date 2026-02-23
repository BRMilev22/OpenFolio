package com.openfolio.export.dto;

public record ExportResponse(
        String token,
        String downloadUrl,
        String template
) {}
