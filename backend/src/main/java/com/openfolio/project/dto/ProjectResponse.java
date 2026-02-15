package com.openfolio.project.dto;

import com.openfolio.project.Project;

import java.util.List;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String url,
        List<String> languages,
        int stars,
        int forks,
        boolean highlighted,
        int displayOrder) {

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getUrl(),
                p.getLanguages(),
                p.getStars(),
                p.getForks(),
                p.isHighlighted(),
                p.getDisplayOrder());
    }
}
