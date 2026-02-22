package com.openfolio.resume.dto;

public record CreateResumeRequest(
        Long portfolioId,
        String title
) {}
