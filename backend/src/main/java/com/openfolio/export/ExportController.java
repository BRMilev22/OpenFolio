package com.openfolio.export;

import com.openfolio.export.dto.ExportOptions;
import com.openfolio.export.dto.ExportResponse;
import com.openfolio.export.dto.SavedResumeInfo;
import com.openfolio.shared.exception.ResourceNotFoundException;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        boolean warm = exportService.isAiCacheWarm(id, user.userId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("ready", warm)));
    }

    /** Pre-warm AI cache asynchronously. Returns immediately. */
    @PostMapping("/api/v1/portfolios/{id}/export/warm-ai")
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
    public ResponseEntity<ApiResponse<List<SavedResumeInfo>>> listSaved(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<SavedResumeInfo> list = exportService.listSaved(user.userId())
                .stream().map(this::toInfo).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    /** Download a saved resume PDF. */
    @GetMapping("/api/v1/saved-resumes/{id}/pdf")
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
    public ResponseEntity<ApiResponse<Void>> deleteSaved(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        exportService.deleteSaved(id, user.userId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Publish a Saved Resume ──────────────────────────────────────

    /** Publish a saved resume — returns a shareable public URL. */
    @PostMapping("/api/v1/saved-resumes/{id}/publish")
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
