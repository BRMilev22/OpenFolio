package com.openfolio.ingestion;

import com.openfolio.ai.AiResumeEnhancer;
import com.openfolio.auth.AuthIdentityRepository;
import com.openfolio.auth.AuthProvider;
import com.openfolio.certification.CertificationRepository;
import com.openfolio.education.EducationRepository;
import com.openfolio.experience.ExperienceRepository;
import com.openfolio.ingestion.dto.IngestionRequest;
import com.openfolio.ingestion.github.GitHubClient;
import com.openfolio.ingestion.github.dto.GitHubRepo;
import com.openfolio.ingestion.github.dto.GitHubUser;
import com.openfolio.portfolio.Portfolio;
import com.openfolio.portfolio.PortfolioRepository;
import com.openfolio.portfolio.PortfolioService;
import com.openfolio.portfolio.dto.PortfolioSummaryResponse;
import com.openfolio.project.Project;
import com.openfolio.project.ProjectRepository;
import com.openfolio.publish.PublishRecordRepository;
import com.openfolio.section.SectionRepository;
import com.openfolio.section.SectionType;
import com.openfolio.skill.Proficiency;
import com.openfolio.skill.Skill;
import com.openfolio.skill.SkillRepository;
import com.openfolio.user.User;
import com.openfolio.user.UserRepository;
import com.openfolio.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final GitHubClient gitHubClient;
    private final PortfolioService portfolioService;
    private final PortfolioRepository portfolioRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final AiResumeEnhancer aiEnhancer;
    private final PublishRecordRepository publishRecordRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final CertificationRepository certificationRepository;
    private final AuthIdentityRepository authIdentityRepository;

    public IngestionService(GitHubClient gitHubClient,
                            PortfolioService portfolioService,
                            PortfolioRepository portfolioRepository,
                            ProjectRepository projectRepository,
                            SkillRepository skillRepository,
                            SectionRepository sectionRepository,
                            UserRepository userRepository,
                            AiResumeEnhancer aiEnhancer,
                            PublishRecordRepository publishRecordRepository,
                            EducationRepository educationRepository,
                            ExperienceRepository experienceRepository,
                            CertificationRepository certificationRepository,
                            AuthIdentityRepository authIdentityRepository) {
        this.gitHubClient = gitHubClient;
        this.portfolioService = portfolioService;
        this.portfolioRepository = portfolioRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.sectionRepository = sectionRepository;
        this.userRepository = userRepository;
        this.aiEnhancer = aiEnhancer;
        this.publishRecordRepository = publishRecordRepository;
        this.educationRepository = educationRepository;
        this.experienceRepository = experienceRepository;
        this.certificationRepository = certificationRepository;
        this.authIdentityRepository = authIdentityRepository;
    }

    @Transactional
    public PortfolioSummaryResponse ingestFromGitHub(Long userId, IngestionRequest request) {
        String username = request.githubUsername();

        // Resolve the best available GitHub token: prefer the user's own OAuth token
        // (stored at login time), fall back to the server-level GITHUB_TOKEN env var,
        // fall back to unauthenticated (60 req/hr — will likely rate-limit).
        String githubToken = authIdentityRepository
                .findByUserIdAndProvider(userId, AuthProvider.GITHUB)
                .map(ai -> ai.getAccessToken())
                .orElse(null);
        GitHubClient gh = gitHubClient.withUserToken(githubToken);
        if (githubToken != null) {
            log.info("Using user GitHub OAuth token for ingestion");
        }

        // 1. Fetch GitHub profile + repos
        GitHubUser ghUser = gh.fetchUser(username);
        List<GitHubRepo> allRepos = gh.fetchRepos(username);

        // 2. Fetch profile README (special {username}/{username} repo)
        String profileReadme = gh.fetchProfileReadme(username);
        log.info("Profile README found: {}", profileReadme != null);

        // 3. Filter: non-fork, non-archived, sorted by stars desc
        List<GitHubRepo> repos = allRepos.stream()
                .filter(r -> !r.fork() && !r.archived())
                .sorted(Comparator.comparingInt(GitHubRepo::stars).reversed())
                .toList();

        // 4. Parallel-fetch language bytes for top 30 repos (more accurate than primary lang)
        Map<String, Long> totalLangBytes = fetchAggregatedLanguages(username, repos, gh);
        log.info("Aggregated {} languages from GitHub languages API", totalLangBytes.size());

        // 5. Lock the user row to serialize concurrent ingestion for the same account,
        //    then save github_username for future re-syncs.
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setGithubUsername(username);
        userRepository.save(user);

        // 6. Re-import strategy: PRESERVE the existing portfolio row (keeps its ID,
        //    AI cache, and user-added experience/education/certifications).
        //    Only refresh GitHub-derived data: projects, skills, sections.
        List<Portfolio> existingPortfolios = portfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        // 7. Build portfolio metadata
        String displayName = ghUser.name() != null && !ghUser.name().isBlank()
                ? ghUser.name() : username;
        String title = displayName + "'s Portfolio";
        String tagline = ghUser.bio() != null && !ghUser.bio().isBlank()
                ? ghUser.bio()
                : "Software developer passionate about building great products.";

        Portfolio portfolio;
        if (!existingPortfolios.isEmpty()) {
            // Re-use the first portfolio — clear only GitHub-derived children
            portfolio = existingPortfolios.get(0);
            Long pid = portfolio.getId();
            projectRepository.deleteAll(projectRepository.findAllByPortfolioIdOrderByDisplayOrder(pid));
            skillRepository.deleteAll(skillRepository.findAllByPortfolioIdOrderByDisplayOrder(pid));
            sectionRepository.deleteAll(sectionRepository.findAllByPortfolioIdOrderByDisplayOrder(pid));
            // DO NOT delete experience, education, certifications — those are user data
            // Update portfolio metadata from GitHub
            portfolio.setTitle(title);
            portfolio.setTagline(tagline);
            portfolioRepository.save(portfolio);
            // Re-create default sections (About, Projects etc.)
            portfolioService.createDefaultSections(portfolio);
            log.info("Re-imported portfolio {} for user {} (preserved user data)", pid, userId);

            // Delete any extra portfolios (shouldn't happen, but clean up)
            for (int i = 1; i < existingPortfolios.size(); i++) {
                Portfolio extra = existingPortfolios.get(i);
                Long epid = extra.getId();
                publishRecordRepository.deleteAll(publishRecordRepository.findAllByPortfolioId(epid));
                projectRepository.deleteAll(projectRepository.findAllByPortfolioIdOrderByDisplayOrder(epid));
                skillRepository.deleteAll(skillRepository.findAllByPortfolioIdOrderByDisplayOrder(epid));
                educationRepository.deleteAll(educationRepository.findAllByPortfolioIdOrderByDisplayOrder(epid));
                experienceRepository.deleteAll(experienceRepository.findAllByPortfolioIdOrderByDisplayOrder(epid));
                certificationRepository.deleteAll(certificationRepository.findAllByPortfolioIdOrderByDisplayOrder(epid));
                sectionRepository.deleteAll(sectionRepository.findAllByPortfolioIdOrderByDisplayOrder(epid));
                portfolioRepository.delete(extra);
            }
        } else {
            // 8. First import — create portfolio + default sections
            portfolio = portfolioService.createForIngestion(user, username, title, tagline);
        }

        // 9. Populate About section with profile README content
        if (profileReadme != null && !profileReadme.isBlank()) {
            sectionRepository.findByPortfolioIdAndType(portfolio.getId(), SectionType.ABOUT)
                    .ifPresent(section -> {
                        section.setContent(cleanMarkdown(profileReadme));
                        sectionRepository.save(section);
                    });
        }

        // 10. Create project entities for all qualifying repos
        for (int i = 0; i < repos.size(); i++) {
            GitHubRepo repo = repos.get(i);
            Project project = new Project();
            project.setPortfolio(portfolio);
            project.setGithubRepoId(repo.id() != null ? repo.id().toString() : null);
            project.setName(repo.name());
            project.setDescription(repo.description());
            project.setUrl(repo.htmlUrl());
            project.setLanguages(repo.language() != null ? List.of(repo.language()) : List.of());
            project.setStars(repo.stars());
            project.setForks(repo.forks());
            project.setHighlighted(i < 6);
            project.setDisplayOrder(i);
            projectRepository.save(project);
        }

        // 11. Build skills from aggregated language bytes (fallback: repo primary lang counts)
        Map<String, Long> langData = totalLangBytes.isEmpty()
                ? buildFallbackLangCounts(repos) : totalLangBytes;

        List<Map.Entry<String, Long>> sortedLangs = langData.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        long maxValue = sortedLangs.isEmpty() ? 1L : sortedLangs.get(0).getValue();
        int skillOrder = 0;
        for (Map.Entry<String, Long> entry : sortedLangs) {
            Skill skill = new Skill();
            skill.setPortfolio(portfolio);
            skill.setName(entry.getKey());
            skill.setCategory("Language");
            skill.setProficiency(inferProficiency(entry.getValue(), maxValue, totalLangBytes.isEmpty()));
            skill.setDisplayOrder(skillOrder++);
            skillRepository.save(skill);
        }

        // 12. AI-enhance content in parallel using Ollama (qwen2.5:14b)
        List<String> topLanguageNames = sortedLangs.stream()
                .limit(6).map(Map.Entry::getKey).toList();

        // Kick off summary enhancement
        CompletableFuture<Void> summaryFuture = CompletableFuture.runAsync(() -> {
            String aiSummary = aiEnhancer.enhanceProfessionalSummary(
                    displayName, profileReadme, topLanguageNames);
            if (aiSummary != null && !aiSummary.isBlank()) {
                sectionRepository.findByPortfolioIdAndType(portfolio.getId(), SectionType.ABOUT)
                        .ifPresent(section -> {
                            section.setContent(aiSummary);
                            sectionRepository.save(section);
                        });
                log.info("AI summary saved for portfolio {}", portfolio.getId());
            }
        });

        // Kick off project description enhancements for top 5 highlighted projects.
        // Capture only IDs + plain values — NOT the JPA entity itself — to avoid
        // ObjectOptimisticLockingFailureException from stale detached references.
        List<Project> savedProjects = projectRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId());
        List<CompletableFuture<Void>> projectFutures = savedProjects.stream()
                .filter(Project::isHighlighted)
                .limit(5)
                .map(proj -> {
                    final Long projId      = proj.getId();
                    final String projName  = proj.getName();
                    final String projDesc  = proj.getDescription();
                    final List<String> projLangs = proj.getLanguages() != null
                            ? new ArrayList<>(proj.getLanguages()) : List.of();
                    final int projStars    = proj.getStars();
                    return CompletableFuture.runAsync(() -> {
                        String aiDesc = aiEnhancer.enhanceProjectDescription(
                                projName, projDesc, projLangs, projStars);
                        if (aiDesc != null && !aiDesc.isBlank()) {
                            // Re-fetch a fresh managed entity — avoids optimistic lock on stale proxy
                            projectRepository.findById(projId).ifPresent(freshProj -> {
                                freshProj.setDescription(aiDesc);
                                projectRepository.save(freshProj);
                            });
                        }
                    });
                })
                .toList();

        // Fire-and-forget: AI runs in background — don't block the HTTP response.
        // The portfolio is returned immediately with raw GitHub data.
        // AI descriptions/summary will be saved within ~30-60s automatically.
        Long portfolioId = portfolio.getId();
        CompletableFuture.allOf(
                        Stream.concat(Stream.of(summaryFuture), projectFutures.stream())
                                .toArray(CompletableFuture[]::new))
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        log.warn("AI enhancement failed for portfolio {} — raw data kept: {}",
                                portfolioId, ex.getMessage());
                    } else {
                        log.info("AI enhancement complete for portfolio {}", portfolioId);
                    }
                });

        return PortfolioSummaryResponse.from(portfolio,
                projectRepository.countByPortfolioId(portfolio.getId()),
                skillRepository.countByPortfolioId(portfolio.getId()));
    }

    private Map<String, Long> fetchAggregatedLanguages(String username, List<GitHubRepo> repos,
                                                        GitHubClient gh) {
        List<GitHubRepo> topRepos = repos.stream().limit(30).toList();
        List<CompletableFuture<Map<String, Long>>> futures = topRepos.stream()
                .map(repo -> CompletableFuture.supplyAsync(
                        () -> gh.fetchRepoLanguages(username, repo.name())))
                .toList();
        Map<String, Long> totals = new LinkedHashMap<>();
        for (CompletableFuture<Map<String, Long>> future : futures) {
            try {
                future.get().forEach((lang, bytes) -> totals.merge(lang, bytes, Long::sum));
            } catch (Exception e) {
                log.warn("Language fetch failed: {}", e.getMessage());
            }
        }
        return totals;
    }

    private Map<String, Long> buildFallbackLangCounts(List<GitHubRepo> repos) {
        return repos.stream()
                .filter(r -> r.language() != null)
                .collect(Collectors.groupingBy(GitHubRepo::language, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Proficiency inferProficiency(long value, long maxValue, boolean isCount) {
        if (isCount) {
            if (value >= 10) return Proficiency.EXPERT;
            if (value >= 5)  return Proficiency.ADVANCED;
            if (value >= 2)  return Proficiency.INTERMEDIATE;
            return Proficiency.BEGINNER;
        }
        double pct = (double) value / maxValue * 100;
        if (pct >= 40) return Proficiency.EXPERT;
        if (pct >= 20) return Proficiency.ADVANCED;
        if (pct >= 8)  return Proficiency.INTERMEDIATE;
        return Proficiency.BEGINNER;
    }

    private String cleanMarkdown(String raw) {
        return raw
                .replaceAll("(?s)```.*?```", "")
                .replaceAll("`[^`]+`", "")
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
                .replaceAll("#{1,6}\\s+", "")
                .replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1")
                .replaceAll("-{3,}|={3,}", "")
                .replaceAll("(?m)^[-*+]\\s+", "• ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
