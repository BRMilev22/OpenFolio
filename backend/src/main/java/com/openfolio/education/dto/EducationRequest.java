package com.openfolio.education.dto;

public record EducationRequest(
        String institution,
        String degree,
        String field,
        Integer startYear,
        Integer endYear,
        Integer displayOrder) {}
