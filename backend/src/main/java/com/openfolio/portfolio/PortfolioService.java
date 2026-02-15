package com.openfolio.portfolio;

import com.openfolio.portfolio.dto.CreatePortfolioRequest;
import com.openfolio.portfolio.dto.PortfolioSummaryResponse;
import com.openfolio.portfolio.dto.UpdatePortfolioRequest;
import com.openfolio.project.ProjectRepository;
import com.openfolio.section.Section;
import com.openfolio.section.SectionRepository;
import com.openfolio.section.SectionType;
import com.openfolio.shared.exception.ResourceNotFoundException;
import com.openfolio.shared.exception.UnauthorizedException;
import com.openfolio.skill.SkillRepository;
import com.openfolio.user.User;
import com.openfolio.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;

    public PortfolioService(PortfolioRepository portfolioRepository,
                            UserRepository userRepository,
                            SectionRepository sectionRepository,
                            ProjectRepository projectRepository,
                            SkillRepository skillRepository) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
        this.sectionRepository = sectionRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<PortfolioSummaryResponse> getPortfolios(Long userId) {
        return portfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(p -> PortfolioSummaryResponse.from(
                        p,
                        projectRepository.countByPortfolioId(p.getId()),
                        skillRepository.countByPortfolioId(p.getId())))
                .toList();
    }

    @Transactional
    public PortfolioSummaryResponse create(Long userId, CreatePortfolioRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Portfolio portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolio.setSlug(generateUniqueSlug(request.title()));
        portfolio.setTitle(request.title());
        portfolio.setTagline(request.tagline());
        portfolio.setThemeKey("dark");
        portfolio = portfolioRepository.save(portfolio);
        createDefaultSections(portfolio);
        return PortfolioSummaryResponse.from(portfolio, 0, 0);
    }

    @Transactional
    public PortfolioSummaryResponse update(Long portfolioId, Long userId, UpdatePortfolioRequest request) {
        Portfolio portfolio = findOwnedPortfolio(portfolioId, userId);
        if (request.title() != null) portfolio.setTitle(request.title());
        if (request.tagline() != null) portfolio.setTagline(request.tagline());
        if (request.themeKey() != null) portfolio.setThemeKey(request.themeKey());
        if (request.published() != null) portfolio.setPublished(request.published());
        portfolio = portfolioRepository.save(portfolio);
        return PortfolioSummaryResponse.from(
                portfolio,
                projectRepository.countByPortfolioId(portfolio.getId()),
                skillRepository.countByPortfolioId(portfolio.getId()));
    }

    @Transactional
    public void delete(Long portfolioId, Long userId) {
        Portfolio portfolio = findOwnedPortfolio(portfolioId, userId);
        portfolioRepository.delete(portfolio);
    }

    /** Package-visible: called by IngestionService */
    @Transactional
    public Portfolio createForIngestion(User user, String baseSlug, String title, String tagline) {
        Portfolio portfolio = new Portfolio();
        portfolio.setUser(user);
        portfolio.setSlug(generateUniqueSlug(baseSlug));
        portfolio.setTitle(title);
        portfolio.setTagline(tagline);
        portfolio.setThemeKey("dark");
        portfolio = portfolioRepository.save(portfolio);
        createDefaultSections(portfolio);
        return portfolio;
    }

    private Portfolio findOwnedPortfolio(Long portfolioId, Long userId) {
        Portfolio p = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        if (!p.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return p;
    }

    private String generateUniqueSlug(String base) {
        String clean = base.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        String slug = clean.isBlank() ? "portfolio" : clean;
        if (slug.length() > 80) slug = slug.substring(0, 80);
        if (!portfolioRepository.existsBySlug(slug)) return slug;
        return slug + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private record SectionDef(String title, SectionType type, int order) {}

    private static final List<SectionDef> DEFAULT_SECTIONS = List.of(
            new SectionDef("About", SectionType.ABOUT, 0),
            new SectionDef("Projects", SectionType.PROJECTS, 1),
            new SectionDef("Skills", SectionType.SKILLS, 2),
            new SectionDef("Experience", SectionType.EXPERIENCE, 3),
            new SectionDef("Education", SectionType.EDUCATION, 4),
            new SectionDef("Contact", SectionType.CONTACT, 5)
    );

    public void createDefaultSections(Portfolio portfolio) {
        DEFAULT_SECTIONS.forEach(def -> {
            Section s = new Section();
            s.setPortfolio(portfolio);
            s.setTitle(def.title());
            s.setType(def.type());
            s.setDisplayOrder(def.order());
            sectionRepository.save(s);
        });
    }
}
