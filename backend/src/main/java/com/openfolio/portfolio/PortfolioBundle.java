package com.openfolio.portfolio;

import com.openfolio.certification.Certification;
import com.openfolio.education.Education;
import com.openfolio.experience.Experience;
import com.openfolio.project.Project;
import com.openfolio.skill.Skill;
import com.openfolio.user.User;

import java.util.List;

/**
 * All data needed to render a portfolio — used by HTML generator, preview, and PDF export.
 */
public record PortfolioBundle(
        Portfolio portfolio,
        User user,
        String aboutContent,
        List<Project> projects,
        List<Skill> skills,
        List<Experience> experiences,
        List<Education> educations,
        List<Certification> certifications
) {}
