package com.openfolio.skill.dto;

import com.openfolio.skill.Proficiency;
import com.openfolio.skill.Skill;

public record SkillResponse(
        Long id,
        String name,
        String category,
        Proficiency proficiency,
        int displayOrder) {

    public static SkillResponse from(Skill s) {
        return new SkillResponse(s.getId(), s.getName(), s.getCategory(), s.getProficiency(), s.getDisplayOrder());
    }
}
