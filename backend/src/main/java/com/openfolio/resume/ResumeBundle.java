package com.openfolio.resume;

import com.openfolio.education.Education;
import com.openfolio.experience.Experience;
import com.openfolio.project.Project;
import com.openfolio.skill.Skill;
import com.openfolio.user.User;

import java.util.List;

/**
 * All data needed to render a resume.
 */
public record ResumeBundle(
        Resume resume,
        User user,
        String aboutContent,
        List<Project> projects,
        List<Skill> skills,
        List<Experience> experiences,
        List<Education> educations
) {}
