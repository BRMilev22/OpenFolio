package com.openfolio.resume.dto;

import com.openfolio.resume.Resume;

import java.time.LocalDateTime;

public record ResumeResponse(
        Long id,
        Long portfolioId,
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
        String selectedEducationIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResumeResponse from(Resume r) {
        return new ResumeResponse(
                r.getId(),
                r.getPortfolio().getId(),
                r.getTitle(),
                r.getTemplateKey(),
                r.getFullName(),
                r.getJobTitle(),
                r.getEmail(),
                r.getPhone(),
                r.getLocation(),
                r.getWebsite(),
                r.getLinkedinUrl(),
                r.getGithubUrl(),
                r.getSummary(),
                r.getSelectedProjectIds(),
                r.getSelectedSkillIds(),
                r.getSelectedExperienceIds(),
                r.getSelectedEducationIds(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
