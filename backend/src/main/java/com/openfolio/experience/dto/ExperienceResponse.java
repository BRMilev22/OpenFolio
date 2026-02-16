package com.openfolio.experience.dto;

import com.openfolio.experience.Experience;

import java.time.LocalDate;

public record ExperienceResponse(
        Long id,
        String company,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean current,
        int displayOrder) {

    public static ExperienceResponse from(Experience e) {
        return new ExperienceResponse(
                e.getId(), e.getCompany(), e.getTitle(), e.getDescription(),
                e.getStartDate(), e.getEndDate(), e.isCurrent(), e.getDisplayOrder());
    }
}
