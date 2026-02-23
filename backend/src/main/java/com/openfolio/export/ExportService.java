package com.openfolio.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openfolio.ai.AiResumeEnhancer;
import com.openfolio.export.dto.ExportOptions;
import com.openfolio.export.dto.ExportResponse;
import com.openfolio.portfolio.Portfolio;
import com.openfolio.portfolio.PortfolioBundle;
import com.openfolio.portfolio.PortfolioDataLoader;
import com.openfolio.portfolio.PortfolioHtmlGenerator;
import com.openfolio.portfolio.PortfolioRepository;
import com.openfolio.project.Project;
import com.openfolio.project.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final PortfolioDataLoader dataLoader;
    private final PortfolioHtmlGenerator htmlGenerator;
    private final ExportTempStore tempStore;
    private final AiResumeEnhancer aiEnhancer;
    private final ProjectRepository projectRepository;
    private final PortfolioRepository portfolioRepository;
    private final SavedResumeRepository savedResumeRepository;

    /** Thread pool for parallel AI enhancement — process all projects simultaneously. */
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(6);

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ExportService(PortfolioDataLoader dataLoader,
                         PortfolioHtmlGenerator htmlGenerator,
                         ExportTempStore tempStore,
                         AiResumeEnhancer aiEnhancer,
                         ProjectRepository projectRepository,
                         PortfolioRepository portfolioRepository,
                         SavedResumeRepository savedResumeRepository) {
        this.dataLoader = dataLoader;
        this.htmlGenerator = htmlGenerator;
        this.tempStore = tempStore;
        this.aiEnhancer = aiEnhancer;
        this.projectRepository = projectRepository;
        this.portfolioRepository = portfolioRepository;
        this.savedResumeRepository = savedResumeRepository;
    }

    @Transactional
    public ExportResponse generatePdf(Long portfolioId, Long userId, String template,
                                       ExportOptions options) {
        PortfolioBundle bundle = dataLoader.load(portfolioId, userId);
        String themeKey = (template != null && !template.isBlank()) ? template.toLowerCase() : "pdf";

        if (options.aiRewriteDescriptions()) {
            bundle = enhanceBundle(bundle);
        }

        String html = htmlGenerator.generateForPdf(bundle, themeKey, options);

        byte[] pdfBytes = renderHtmlToPdf(html);
        String token = tempStore.store(pdfBytes);
        String downloadUrl = baseUrl + "/api/v1/export/download/" + token;

        log.info("Generated PDF for portfolio {} ({}KB), template={}, token={}",
                portfolioId, pdfBytes.length / 1024, themeKey, token);
        return new ExportResponse(token, downloadUrl, themeKey);
    }

    /**
     * Generate PDF bytes for in-app viewing (returns raw byte array).
     */
    @Transactional
    public byte[] generatePdfBytes(Long portfolioId, Long userId, String template,
                                    ExportOptions options) {
        PortfolioBundle bundle = dataLoader.load(portfolioId, userId);
        String themeKey = (template != null && !template.isBlank()) ? template.toLowerCase() : "pdf";

        if (options.aiRewriteDescriptions()) {
            bundle = enhanceBundle(bundle);
        }

        String html = htmlGenerator.generateForPdf(bundle, themeKey, options);
        return renderHtmlToPdf(html);
    }

    /**
     * Generate preview HTML that matches the PDF layout — for in-app preview.
     */
    @Transactional
    public String generatePreviewHtml(Long portfolioId, Long userId, String template,
                                       ExportOptions options) {
        PortfolioBundle bundle = dataLoader.load(portfolioId, userId);
        String themeKey = (template != null && !template.isBlank()) ? template.toLowerCase() : "pdf";

        if (options.aiRewriteDescriptions()) {
            bundle = enhanceBundle(bundle);
        }

        return htmlGenerator.generateForPdf(bundle, themeKey, options);
    }

    /**
     * Check if AI cache is warm for a portfolio (all projects + summary already enhanced).
     */
    public boolean isAiCacheWarm(Long portfolioId, Long userId) {
        PortfolioBundle bundle = dataLoader.load(portfolioId, userId);
        if (bundle.portfolio().getAiEnhancedSummary() == null
                || bundle.portfolio().getAiEnhancedSummary().isBlank()) {
            return false;
        }
        for (Project p : bundle.projects()) {
            if (p.getDescription() != null && !p.getDescription().isBlank()
                    && (p.getAiEnhancedDescription() == null || p.getAiEnhancedDescription().isBlank())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pre-warm the AI cache by running all enhancements now.
     * Called asynchronously from the controller so the client doesn't block.
     */
    @Transactional
    public void warmUpAiCache(Long portfolioId, Long userId) {
        PortfolioBundle bundle = dataLoader.load(portfolioId, userId);
        enhanceBundle(bundle);
        log.info("AI cache warmed for portfolio {}", portfolioId);
    }

    /**
     * AI-enhance the entire bundle: professional summary + project descriptions.
     * Results are persisted to the database so they survive server restarts.
     * Returns a NEW PortfolioBundle with enhanced content.
     */
    private PortfolioBundle enhanceBundle(PortfolioBundle bundle) {
        // 1. Enhance project descriptions (parallel, DB-cached)
        enhanceProjectDescriptions(bundle.projects());

        // 2. Enhance professional summary (DB-cached)
        String enhancedSummary = enhanceSummary(bundle);

        // Return new bundle with enhanced summary
        if (enhancedSummary != null && !enhancedSummary.equals(bundle.aboutContent())) {
            return new PortfolioBundle(
                    bundle.portfolio(), bundle.user(), enhancedSummary,
                    bundle.projects(), bundle.skills(),
                    bundle.experiences(), bundle.educations(),
                    bundle.certifications()
            );
        }
        return bundle;
    }

    /**
     * AI-enhance the professional summary. Checks the database first —
     * if the portfolio already has an AI-enhanced summary, use that.
     * Otherwise call Ollama and persist the result.
     */
    private String enhanceSummary(PortfolioBundle bundle) {
        Portfolio portfolio = bundle.portfolio();

        // Check DB cache first
        if (portfolio.getAiEnhancedSummary() != null && !portfolio.getAiEnhancedSummary().isBlank()) {
            log.info("Using cached AI summary for portfolio {}", portfolio.getId());
            return portfolio.getAiEnhancedSummary();
        }

        String rawSummary = bundle.aboutContent();
        if (rawSummary == null || rawSummary.isBlank()) return rawSummary;

        String displayName = bundle.user() != null && bundle.user().getDisplayName() != null
                ? bundle.user().getDisplayName() : "Developer";
        List<String> topLangs = bundle.skills().stream()
                .limit(6).map(s -> s.getName()).toList();

        log.info("AI-enhancing professional summary for portfolio {}...", portfolio.getId());
        try {
            String enhanced = aiEnhancer.enhanceProfessionalSummary(displayName, rawSummary, topLangs);
            if (enhanced != null && !enhanced.isBlank()) {
                // Persist to database
                portfolio.setAiEnhancedSummary(enhanced);
                portfolio.setAiEnhancedAt(LocalDateTime.now());
                portfolioRepository.save(portfolio);
                log.info("Saved AI summary to DB for portfolio {}", portfolio.getId());
                return enhanced;
            }
        } catch (Exception e) {
            log.warn("AI summary enhancement failed: {}", e.getMessage());
        }
        return rawSummary;
    }

    /**
     * AI-rewrite raw GitHub project descriptions into professional resume bullet points.
     * All projects are processed IN PARALLEL to minimize latency.
     * Results are persisted to the database so they survive server restarts.
     */
    private void enhanceProjectDescriptions(List<Project> projects) {
        // Split into cached (has DB result) and uncached (need AI call)
        List<Project> needAi = new ArrayList<>();
        for (Project p : projects) {
            if (p.getDescription() == null || p.getDescription().isBlank()) continue;

            // Check DB cache first
            if (p.getAiEnhancedDescription() != null && !p.getAiEnhancedDescription().isBlank()) {
                log.debug("Using cached AI description for project {} ({})", p.getId(), p.getName());
                p.setDescription(p.getAiEnhancedDescription());
            } else {
                needAi.add(p);
            }
        }

        if (needAi.isEmpty()) return;

        log.info("AI-enhancing {} project descriptions in parallel...", needAi.size());

        // Fire all AI calls in parallel
        List<CompletableFuture<Void>> futures = needAi.stream()
                .map(p -> CompletableFuture.runAsync(() -> {
                    try {
                        String enhanced = aiEnhancer.enhanceProjectDescription(
                                p.getName(),
                                p.getDescription(),
                                p.getLanguages() != null ? p.getLanguages() : List.of(),
                                p.getStars()
                        );
                        if (enhanced != null && !enhanced.isBlank()) {
                            // Persist to database
                            p.setAiEnhancedDescription(enhanced);
                            p.setAiEnhancedAt(LocalDateTime.now());
                            projectRepository.save(p);
                            // Also set the description for current rendering
                            p.setDescription(enhanced);
                            log.debug("AI-rewritten & saved: {} → {}", p.getName(), enhanced);
                        }
                    } catch (Exception e) {
                        log.warn("AI enhancement failed for {}: {}", p.getName(), e.getMessage());
                    }
                }, aiExecutor))
                .toList();

        // Wait for all to complete (timeout 180s — 20+ projects can take 2+ min with 6 threads)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(180, java.util.concurrent.TimeUnit.SECONDS);
            log.info("All {} AI enhancements completed and saved to DB", needAi.size());
        } catch (Exception e) {
            log.warn("Some AI enhancements timed out: {}", e.getMessage());
        }
    }

    private byte[] renderHtmlToPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("PDF render failed", e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ─── Saved Resumes ──────────────────────────────────────────────────

    /** Generate a PDF and immediately save it to the database. */
    public SavedResume generateAndSavePdf(Long portfolioId, Long userId, String template,
                                           ExportOptions options, String title) {
        byte[] pdfBytes = generatePdfBytes(portfolioId, userId, template, options);
        String themeKey = (template != null && !template.isBlank()) ? template.toLowerCase() : "pdf";
        String resolvedTitle = (title != null && !title.isBlank()) ? title
                : "Resume — " + themeKey.substring(0, 1).toUpperCase() + themeKey.substring(1);
        SavedResume saved = new SavedResume(userId, portfolioId, resolvedTitle, themeKey, pdfBytes);
        savedResumeRepository.save(saved);
        log.info("Saved resume PDF for user {} portfolio {} ({}KB) id={}",
                userId, portfolioId, pdfBytes.length / 1024, saved.getId());
        return saved;
    }

    /** List all saved resumes for a user (metadata only, no PDF bytes). */
    public List<SavedResume> listSaved(Long userId) {
        return savedResumeRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Get a saved resume (with PDF data) — returns null if not found or wrong user. */
    public SavedResume getSaved(Long id, Long userId) {
        return savedResumeRepository.findByIdAndUserId(id, userId).orElse(null);
    }

    /** Delete a saved resume. */
    @Transactional
    public void deleteSaved(Long id, Long userId) {
        savedResumeRepository.deleteByIdAndUserId(id, userId);
    }

    /** Update a saved resume (e.g. publish token). */
    @Transactional
    public void updateSaved(SavedResume saved) {
        savedResumeRepository.save(saved);
    }

    /** Get a saved resume by its public token (no auth needed). */
    public SavedResume getByPublishToken(String token) {
        return savedResumeRepository.findByPublishToken(token).orElse(null);
    }
}
