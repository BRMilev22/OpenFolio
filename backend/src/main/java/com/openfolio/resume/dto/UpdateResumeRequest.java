package com.openfolio.resume.dto;

public record UpdateResumeRequest(
        String title,
        String templateKey,
        String fullName,
        String jobTitle,
        String email,
        String phone,
        String location,
        String website,
        String linkedinUrl,
        String githubUrl,
        String summary,
        String selectedProjectIds,
        String selectedSkillIds,
        String selectedExperienceIds,
        String selectedEducationIds
) {}
