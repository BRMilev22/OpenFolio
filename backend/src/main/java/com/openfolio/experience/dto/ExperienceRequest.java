package com.openfolio.experience.dto;

import java.time.LocalDate;

public record ExperienceRequest(
        String company,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean current,
        Integer displayOrder) {}
