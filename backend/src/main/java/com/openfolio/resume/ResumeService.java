package com.openfolio.resume;

import com.openfolio.education.Education;
import com.openfolio.education.EducationRepository;
import com.openfolio.experience.Experience;
import com.openfolio.experience.ExperienceRepository;
import com.openfolio.export.ExportTempStore;
import com.openfolio.export.dto.ExportResponse;
import com.openfolio.portfolio.Portfolio;
import com.openfolio.portfolio.PortfolioRepository;
import com.openfolio.project.Project;
import com.openfolio.project.ProjectRepository;
import com.openfolio.resume.dto.CreateResumeRequest;
import com.openfolio.resume.dto.ResumeResponse;
import com.openfolio.resume.dto.UpdateResumeRequest;
import com.openfolio.section.SectionRepository;
import com.openfolio.section.SectionType;
import com.openfolio.shared.exception.ResourceNotFoundException;
import com.openfolio.shared.exception.UnauthorizedException;
import com.openfolio.skill.Skill;
import com.openfolio.skill.SkillRepository;
import com.openfolio.user.User;
import com.openfolio.user.UserRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final SectionRepository sectionRepository;
    private final ResumeHtmlGenerator htmlGenerator;
    private final ExportTempStore tempStore;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ResumeService(ResumeRepository resumeRepository,
                         UserRepository userRepository,
                         PortfolioRepository portfolioRepository,
                         ProjectRepository projectRepository,
                         SkillRepository skillRepository,
                         ExperienceRepository experienceRepository,
                         EducationRepository educationRepository,
                         SectionRepository sectionRepository,
                         ResumeHtmlGenerator htmlGenerator,
                         ExportTempStore tempStore) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.portfolioRepository = portfolioRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.sectionRepository = sectionRepository;
        this.htmlGenerator = htmlGenerator;
        this.tempStore = tempStore;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ResumeResponse> list(Long userId) {
        return resumeRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(ResumeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse get(Long resumeId, Long userId) {
        Resume r = findOwned(resumeId, userId);
        return ResumeResponse.from(r);
    }

    @Transactional
    public ResumeResponse create(Long userId, CreateResumeRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Portfolio portfolio = portfolioRepository.findById(req.portfolioId())
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", req.portfolioId()));
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setPortfolio(portfolio);
        resume.setTitle(req.title() != null ? req.title() : "My Resume");

        // Pre-populate with user data
        resume.setFullName(user.getDisplayName());
        resume.setEmail(user.getEmail());
        if (user.getGithubUsername() != null) {
            resume.setGithubUrl("https://github.com/" + user.getGithubUsername());
        }

        // Pre-populate summary from portfolio about section
        String about = sectionRepository
                .findByPortfolioIdAndType(portfolio.getId(), SectionType.ABOUT)
                .map(s -> s.getContent())
                .orElse(null);
        resume.setSummary(about);

        // Select all items by default
        List<Long> projectIds = projectRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId())
                .stream().map(Project::getId).toList();
        List<Long> skillIds = skillRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId())
                .stream().map(Skill::getId).toList();
        List<Long> expIds = experienceRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId())
                .stream().map(Experience::getId).toList();
        List<Long> eduIds = educationRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolio.getId())
                .stream().map(Education::getId).toList();

        resume.setSelectedProjectIds(idsToJson(projectIds));
        resume.setSelectedSkillIds(idsToJson(skillIds));
        resume.setSelectedExperienceIds(idsToJson(expIds));
        resume.setSelectedEducationIds(idsToJson(eduIds));

        resume = resumeRepository.save(resume);
        return ResumeResponse.from(resume);
    }

    @Transactional
    public ResumeResponse update(Long resumeId, Long userId, UpdateResumeRequest req) {
        Resume r = findOwned(resumeId, userId);
        if (req.title() != null) r.setTitle(req.title());
        if (req.templateKey() != null) r.setTemplateKey(req.templateKey());
        if (req.fullName() != null) r.setFullName(req.fullName());
        if (req.jobTitle() != null) r.setJobTitle(req.jobTitle());
        if (req.email() != null) r.setEmail(req.email());
        if (req.phone() != null) r.setPhone(req.phone());
        if (req.location() != null) r.setLocation(req.location());
        if (req.website() != null) r.setWebsite(req.website());
        if (req.linkedinUrl() != null) r.setLinkedinUrl(req.linkedinUrl());
        if (req.githubUrl() != null) r.setGithubUrl(req.githubUrl());
        if (req.summary() != null) r.setSummary(req.summary());
        if (req.selectedProjectIds() != null) r.setSelectedProjectIds(req.selectedProjectIds());
        if (req.selectedSkillIds() != null) r.setSelectedSkillIds(req.selectedSkillIds());
        if (req.selectedExperienceIds() != null) r.setSelectedExperienceIds(req.selectedExperienceIds());
        if (req.selectedEducationIds() != null) r.setSelectedEducationIds(req.selectedEducationIds());
        r = resumeRepository.save(r);
        return ResumeResponse.from(r);
    }

    @Transactional
    public void delete(Long resumeId, Long userId) {
        Resume r = findOwned(resumeId, userId);
        resumeRepository.delete(r);
    }

    // ── Preview & PDF ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public String previewHtml(Long resumeId, Long userId) {
        ResumeBundle bundle = loadBundle(resumeId, userId);
        return htmlGenerator.generate(bundle, bundle.resume().getTemplateKey());
    }

    @Transactional(readOnly = true)
    public ExportResponse generatePdf(Long resumeId, Long userId) {
        ResumeBundle bundle = loadBundle(resumeId, userId);
        String html = htmlGenerator.generateForPdf(bundle, bundle.resume().getTemplateKey());
        byte[] pdf = renderPdf(html);
        String token = tempStore.store(pdf);
        String downloadUrl = baseUrl + "/api/v1/export/download/" + token;
        log.info("Generated resume PDF ({}KB) token={}", pdf.length / 1024, token);
        return new ExportResponse(token, downloadUrl, bundle.resume().getTemplateKey());
    }

    /** Returns PDF as base64 string for in-app rendering. */
    @Transactional(readOnly = true)
    public String generatePdfBase64(Long resumeId, Long userId) {
        ResumeBundle bundle = loadBundle(resumeId, userId);
        String html = htmlGenerator.generateForPdf(bundle, bundle.resume().getTemplateKey());
        byte[] pdf = renderPdf(html);
        return java.util.Base64.getEncoder().encodeToString(pdf);
    }

    /** Preview HTML for a specific template key (without changing the resume's saved template). */
    @Transactional(readOnly = true)
    public String previewHtmlWithTemplate(Long resumeId, Long userId, String templateKey) {
        ResumeBundle bundle = loadBundle(resumeId, userId);
        return htmlGenerator.generate(bundle, templateKey);
    }

    /** Returns available template metadata */
    public List<TemplateInfo> getTemplates() {
        return List.of(
                new TemplateInfo("classic", "Classic", "Clean single-column layout inspired by top tech resumes. ATS-friendly, print-optimised.",
                        "#7C3AED", "📄"),
                new TemplateInfo("modern", "Modern", "Contemporary two-tone header with skill bars and timeline-style experience.",
                        "#2563EB", "✨"),
                new TemplateInfo("minimal", "Minimal", "Elegant whitespace-focused design. Maximum readability, zero distractions.",
                        "#0D9488", "◻️"),
                new TemplateInfo("bold", "Bold", "High-contrast dark header with vivid accent colors. Makes a strong first impression.",
                        "#DC2626", "🔥")
        );
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private Resume findOwned(Long resumeId, Long userId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
    }

    @Transactional(readOnly = true)
    public ResumeBundle loadBundle(Long resumeId, Long userId) {
        Resume resume = findOwned(resumeId, userId);
        User user = resume.getUser();
        user.getDisplayName(); // init lazy proxy
        Long portfolioId = resume.getPortfolio().getId();

        String aboutContent = resume.getSummary();

        List<Project> allProjects = projectRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolioId);
        List<Skill> allSkills = skillRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolioId);
        List<Experience> allExperiences = experienceRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolioId);
        List<Education> allEducations = educationRepository.findAllByPortfolioIdOrderByDisplayOrder(portfolioId);

        // Filter to selected IDs
        List<Long> projIds = jsonToIds(resume.getSelectedProjectIds());
        List<Long> skillIds = jsonToIds(resume.getSelectedSkillIds());
        List<Long> expIds = jsonToIds(resume.getSelectedExperienceIds());
        List<Long> eduIds = jsonToIds(resume.getSelectedEducationIds());

        List<Project> projects = projIds.isEmpty() ? allProjects :
                allProjects.stream().filter(p -> projIds.contains(p.getId())).toList();
        List<Skill> skills = skillIds.isEmpty() ? allSkills :
                allSkills.stream().filter(s -> skillIds.contains(s.getId())).toList();
        List<Experience> experiences = expIds.isEmpty() ? allExperiences :
                allExperiences.stream().filter(e -> expIds.contains(e.getId())).toList();
        List<Education> educations = eduIds.isEmpty() ? allEducations :
                allEducations.stream().filter(e -> eduIds.contains(e.getId())).toList();

        return new ResumeBundle(resume, user, aboutContent, projects, skills, experiences, educations);
    }

    private String idsToJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "[]";
        return "[" + ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("") + "]";
    }

    private List<Long> jsonToIds(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        String clean = json.replaceAll("[\\[\\]\\s]", "");
        if (clean.isBlank()) return List.of();
        List<Long> result = new ArrayList<>();
        for (String s : clean.split(",")) {
            try { result.add(Long.parseLong(s.trim())); } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Resume PDF render failed", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }
}
