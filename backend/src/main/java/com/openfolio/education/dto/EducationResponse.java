package com.openfolio.education.dto;

import com.openfolio.education.Education;

public record EducationResponse(
        Long id,
        String institution,
        String degree,
        String field,
        Integer startYear,
        Integer endYear,
        int displayOrder) {

    public static EducationResponse from(Education e) {
        return new EducationResponse(
                e.getId(), e.getInstitution(), e.getDegree(), e.getField(),
                e.getStartYear(), e.getEndYear(), e.getDisplayOrder());
    }
}
