package com.openfolio.portfolio;

import com.openfolio.project.ProjectRepository;
import com.openfolio.project.dto.ProjectResponse;
import com.openfolio.shared.exception.ResourceNotFoundException;
import com.openfolio.shared.exception.UnauthorizedException;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import com.openfolio.skill.SkillRepository;
import com.openfolio.skill.dto.SkillResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}")
@Tag(name = "Portfolio Items", description = "Projects and skills within a portfolio")
public class PortfolioItemsController {

    private final PortfolioRepository portfolioRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;

    public PortfolioItemsController(PortfolioRepository portfolioRepository,
                                    ProjectRepository projectRepository,
                                    SkillRepository skillRepository) {
        this.portfolioRepository = portfolioRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
    }

    @GetMapping("/projects")
    @Operation(summary = "List projects", description = "Returns all projects in the portfolio ordered by display order.")
    public ApiResponse<List<ProjectResponse>> getProjects(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        verifyOwnership(portfolioId, user.userId());
        return ApiResponse.ok(
                projectRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolioId)
                        .stream().map(ProjectResponse::from).toList());
    }

    @GetMapping("/skills")
    @Operation(summary = "List skills", description = "Returns all skills in the portfolio ordered by display order.")
    public ApiResponse<List<SkillResponse>> getSkills(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        verifyOwnership(portfolioId, user.userId());
        return ApiResponse.ok(
                skillRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolioId)
                        .stream().map(SkillResponse::from).toList());
    }

    private void verifyOwnership(Long portfolioId, Long userId) {
        Portfolio p = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        if (!p.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
    }
}
