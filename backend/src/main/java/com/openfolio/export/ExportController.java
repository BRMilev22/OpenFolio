package com.openfolio.export;

import com.openfolio.export.dto.ExportOptions;
import com.openfolio.export.dto.ExportResponse;
import com.openfolio.export.dto.SavedResumeInfo;
import com.openfolio.shared.exception.ResourceNotFoundException;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "PDF Export", description = "Generate, preview, download, save, and publish PDF resumes")
public class ExportController {

    private final ExportService exportService;
    private final ExportTempStore tempStore;

    @org.springframework.beans.factory.annotation.Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ExportController(ExportService exportService, ExportTempStore tempStore) {
        this.exportService = exportService;
        this.tempStore = tempStore;
    }

    /** Trigger PDF generation → returns a short-lived download token + URL. */
    @PostMapping("/api/v1/portfolios/{id}/export/pdf")
    @Operation(summary = "Generate PDF resume", description = "Renders the portfolio as a PDF and returns a short-lived download token + URL.")
    public ResponseEntity<ApiResponse<ExportResponse>> generatePdf(
            @PathVariable Long id,
            @RequestParam(defaultValue = "pdf") String template,
            @RequestParam(defaultValue = "false") boolean aiRewrite,
            @RequestParam(defaultValue = "false") boolean includePhoto,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(defaultValue = "false") boolean includePhone,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "false") boolean includeLinkedIn,
            @RequestParam(required = false) String linkedIn,
            @RequestParam(defaultValue = "false") boolean includeWebsite,
            @RequestParam(required = false) String website,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ExportOptions options = ExportOptions.of(aiRewrite, includePhoto, photoUrl,
                includePhone, phone, includeLinkedIn, linkedIn, includeWebsite, website);
        ExportResponse response = exportService.generatePdf(id, user.userId(), template, options);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** Preview HTML that matches exactly how the PDF will look. */
    @GetMapping("/api/v1/portfolios/{id}/export/preview")
    @Operation(summary = "Preview PDF as HTML", description = "Returns the HTML that will be rendered to PDF, useful for in-app preview.")
    public ResponseEntity<String> previewHtml(
            @PathVariable Long id,
            @RequestParam(defaultValue = "pdf") String template,
            @RequestParam(defaultValue = "false") boolean aiRewrite,
            @RequestParam(defaultValue = "false") boolean includePhoto,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(defaultValue = "false") boolean includePhone,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "false") boolean includeLinkedIn,
            @RequestParam(required = false) String linkedIn,
            @RequestParam(defaultValue = "false") boolean includeWebsite,
            @RequestParam(required = false) String website,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ExportOptions options = ExportOptions.of(aiRewrite, includePhoto, photoUrl,
                includePhone, phone, includeLinkedIn, linkedIn, includeWebsite, website);
        String html = exportService.generatePreviewHtml(id, user.userId(), template, options);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    /** Check if AI cache is warm for a portfolio. */
    @GetMapping("/api/v1/portfolios/{id}/export/ai-status")
    @Operation(summary = "Check AI rewrite cache", description = "Returns whether the Ollama AI-rewritten content is cached and ready.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        boolean warm = exportService.isAiCacheWarm(id, user.userId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("ready", warm)));
    }

    /** Pre-warm AI cache asynchronously. Returns immediately. */
    @PostMapping("/api/v1/portfolios/{id}/export/warm-ai")
    @Operation(summary = "Warm AI rewrite cache", description = "Triggers an async Ollama call to pre-generate AI-enhanced resume text.")
    public ResponseEntity<ApiResponse<Map<String, String>>> warmAi(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Long userId = user.userId();
        // Run in background so the client doesn't block
        new Thread(() -> {
            try {
                exportService.warmUpAiCache(id, userId);
            } catch (Exception e) {
                // logged inside warmUpAiCache
            }
        }).start();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "warming")));
    }

    /** Generate PDF and return as base64 JSON for in-app viewing. */
    @PostMapping("/api/v1/portfolios/{id}/export/pdf/inline")
    @Operation(summary = "Generate PDF inline (base64)", description = "Generates a PDF and returns it as a base64-encoded string for in-app viewing.")
    public ResponseEntity<ApiResponse<Map<String, String>>> pdfInline(
            @PathVariable Long id,
            @RequestParam(defaultValue = "pdf") String template,
            @RequestParam(defaultValue = "false") boolean aiRewrite,
            @RequestParam(defaultValue = "false") boolean includePhoto,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(defaultValue = "false") boolean includePhone,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "false") boolean includeLinkedIn,
            @RequestParam(required = false) String linkedIn,
            @RequestParam(defaultValue = "false") boolean includeWebsite,
            @RequestParam(required = false) String website,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ExportOptions options = ExportOptions.of(aiRewrite, includePhoto, photoUrl,
                includePhone, phone, includeLinkedIn, linkedIn, includeWebsite, website);
        byte[] pdfBytes = exportService.generatePdfBytes(id, user.userId(), template, options);
        String base64 = Base64.getEncoder().encodeToString(pdfBytes);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("base64", base64)));
    }

    /** Unauthenticated download endpoint (token acts as proof of generation). */
    @GetMapping("/api/v1/export/download/{token}")
    @Operation(summary = "Download PDF by token", description = "Public endpoint — the short-lived token acts as proof of generation.", security = {})
    public ResponseEntity<byte[]> download(@PathVariable String token) {
        byte[] pdf = tempStore.retrieve(token);
        if (pdf == null) throw new ResourceNotFoundException("Export", token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ─── Saved Resumes ──────────────────────────────────────────────────

    /** Generate PDF and save it permanently. Returns metadata (no PDF bytes). */
    @PostMapping("/api/v1/portfolios/{id}/export/save")
    @Operation(summary = "Generate & save PDF", description = "Generates a PDF, saves it persistently, and returns metadata.")
    public ResponseEntity<ApiResponse<SavedResumeInfo>> generateAndSave(
            @PathVariable Long id,
            @RequestParam(defaultValue = "pdf") String template,
            @RequestParam(defaultValue = "false") boolean aiRewrite,
            @RequestParam(defaultValue = "false") boolean includePhoto,
            @RequestParam(required = false) String photoUrl,
            @RequestParam(defaultValue = "false") boolean includePhone,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "false") boolean includeLinkedIn,
            @RequestParam(required = false) String linkedIn,
            @RequestParam(defaultValue = "false") boolean includeWebsite,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String title,
            @AuthenticationPrincipal AuthenticatedUser user) {
        ExportOptions options = ExportOptions.of(aiRewrite, includePhoto, photoUrl,
                includePhone, phone, includeLinkedIn, linkedIn, includeWebsite, website);
        SavedResume saved = exportService.generateAndSavePdf(id, user.userId(), template, options, title);
        return ResponseEntity.ok(ApiResponse.ok(toInfo(saved)));
    }

    /** List all saved resumes for the current user. */
    @GetMapping("/api/v1/saved-resumes")
    @Operation(summary = "List saved resumes", description = "Returns metadata for all saved PDF resumes belonging to the current user.")
    public ResponseEntity<ApiResponse<List<SavedResumeInfo>>> listSaved(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<SavedResumeInfo> list = exportService.listSaved(user.userId())
                .stream().map(this::toInfo).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /** Download a saved resume PDF. */
    @GetMapping("/api/v1/saved-resumes/{id}/pdf")
    @Operation(summary = "Download saved resume", description = "Returns the saved PDF as a binary download.")
    public ResponseEntity<byte[]> downloadSaved(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        SavedResume saved = exportService.getSaved(id, user.userId());
        if (saved == null) throw new ResourceNotFoundException("SavedResume", id.toString());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + saved.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(saved.getPdfData());
    }

    /** Get a saved resume PDF as base64 for in-app viewing. */
    @GetMapping("/api/v1/saved-resumes/{id}/base64")
    @Operation(summary = "Saved resume as base64", description = "Returns the saved PDF as a base64 string for in-app viewing.")
    public ResponseEntity<ApiResponse<Map<String, String>>> savedBase64(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        SavedResume saved = exportService.getSaved(id, user.userId());
        if (saved == null) throw new ResourceNotFoundException("SavedResume", id.toString());
        String b64 = Base64.getEncoder().encodeToString(saved.getPdfData());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("base64", b64)));
    }

    /** Delete a saved resume. */
    @DeleteMapping("/api/v1/saved-resumes/{id}")
    @Operation(summary = "Delete saved resume", description = "Permanently removes a saved PDF resume.")
    public ResponseEntity<ApiResponse<Void>> deleteSaved(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        exportService.deleteSaved(id, user.userId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Publish a Saved Resume ──────────────────────────────────────

    /** Publish a saved resume — returns a shareable public URL. */
    @PostMapping("/api/v1/saved-resumes/{id}/publish")
    @Operation(summary = "Publish saved resume", description = "Generates a public URL token so anyone with the link can view the PDF.")
    public ResponseEntity<ApiResponse<SavedResumeInfo>> publishResume(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        SavedResume saved = exportService.getSaved(id, user.userId());
        if (saved == null) throw new ResourceNotFoundException("SavedResume", id.toString());

        if (saved.getPublishToken() == null) {
            saved.setPublishToken(UUID.randomUUID().toString().replace("-", ""));
            saved.setPublishedAt(LocalDateTime.now());
            exportService.updateSaved(saved);
        }
        return ResponseEntity.ok(ApiResponse.ok(toInfo(saved)));
    }

    /** Unpublish a saved resume — removes the public link. */
    @DeleteMapping("/api/v1/saved-resumes/{id}/publish")
    @Operation(summary = "Unpublish saved resume", description = "Deactivates the public sharing link.")
    public ResponseEntity<ApiResponse<SavedResumeInfo>> unpublishResume(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        SavedResume saved = exportService.getSaved(id, user.userId());
        if (saved == null) throw new ResourceNotFoundException("SavedResume", id.toString());

        saved.setPublishToken(null);
        saved.setPublishedAt(null);
        exportService.updateSaved(saved);
        return ResponseEntity.ok(ApiResponse.ok(toInfo(saved)));
    }

    /** Public endpoint — anyone with the token can view the PDF. */
    @GetMapping("/api/v1/public/resume/{token}")
    @Operation(summary = "View published resume PDF", description = "Public endpoint — anyone with the token can view/download the PDF.", security = {})
    public ResponseEntity<byte[]> publicResumePdf(@PathVariable String token) {
        SavedResume saved = exportService.getByPublishToken(token);
        if (saved == null) throw new ResourceNotFoundException("Resume", token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + saved.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(saved.getPdfData());
    }

    private SavedResumeInfo toInfo(SavedResume s) {
        String publicUrl = s.getPublishToken() != null
                ? baseUrl + "/api/v1/public/resume/" + s.getPublishToken()
                : null;
        return new SavedResumeInfo(s.getId(), s.getPortfolioId(), s.getTitle(),
                s.getTemplateKey(), s.getFileSizeBytes(), s.getCreatedAt(), publicUrl);
    }
}
