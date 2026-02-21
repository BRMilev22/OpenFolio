package com.openfolio.portfolio;

import com.openfolio.certification.Certification;
import com.openfolio.certification.CertificationRepository;
import com.openfolio.education.Education;
import com.openfolio.education.EducationRepository;
import com.openfolio.experience.Experience;
import com.openfolio.experience.ExperienceRepository;
import com.openfolio.project.Project;
import com.openfolio.project.ProjectRepository;
import com.openfolio.section.SectionRepository;
import com.openfolio.section.SectionType;
import com.openfolio.skill.Skill;
import com.openfolio.skill.SkillRepository;
import com.openfolio.shared.exception.ResourceNotFoundException;
import com.openfolio.shared.exception.UnauthorizedException;
import com.openfolio.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Loads all data for a portfolio into a PortfolioBundle, verifying ownership.
 * Used by Preview, Export, and Public endpoints.
 */
@Service
public class PortfolioDataLoader {

    private final PortfolioRepository portfolioRepository;
    private final SectionRepository sectionRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final CertificationRepository certificationRepository;

    public PortfolioDataLoader(PortfolioRepository portfolioRepository,
                               SectionRepository sectionRepository,
                               ProjectRepository projectRepository,
                               SkillRepository skillRepository,
                               ExperienceRepository experienceRepository,
                               EducationRepository educationRepository,
                               CertificationRepository certificationRepository) {
        this.portfolioRepository = portfolioRepository;
        this.sectionRepository = sectionRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.certificationRepository = certificationRepository;
    }

    /** Load bundle and verify the requesting user owns it. */
    @Transactional(readOnly = true)
    public PortfolioBundle load(Long portfolioId, Long userId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return buildBundle(portfolio);
    }

    /** Load bundle for a published portfolio by slug (no auth check). */
    @Transactional(readOnly = true)
    public PortfolioBundle loadBySlug(String slug) {
        Portfolio portfolio = portfolioRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", slug));
        if (!portfolio.isPublished()) {
            throw new ResourceNotFoundException("Portfolio", slug);
        }
        return buildBundle(portfolio);
    }

    private PortfolioBundle buildBundle(Portfolio portfolio) {
        // Eagerly initialize the User proxy while the Hibernate session is still open.
        // portfolio.getUser() is a lazy proxy; accessing any field forces loading.
        User user = portfolio.getUser();
        user.getDisplayName(); // trigger proxy initialization within transaction
        String aboutContent = sectionRepository
                .findByPortfolioIdAndType(portfolio.getId(), SectionType.ABOUT)
                .map(s -> s.getContent())
                .orElse(null);
        List<Project> projects = projectRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId());
        List<Skill> skills = skillRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId());
        List<Experience> experiences = experienceRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId());
        List<Education> educations = educationRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId());
        List<Certification> certifications = certificationRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId());
        return new PortfolioBundle(portfolio, user, aboutContent, projects, skills, experiences, educations, certifications);
    }
}
