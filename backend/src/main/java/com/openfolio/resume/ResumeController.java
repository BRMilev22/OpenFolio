package com.openfolio.resume;

import com.openfolio.export.dto.ExportResponse;
import com.openfolio.resume.dto.CreateResumeRequest;
import com.openfolio.resume.dto.ResumeResponse;
import com.openfolio.resume.dto.UpdateResumeRequest;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    /** List all resumes for the current user. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.list(user.userId())));
    }

    /** Get a single resume by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> get(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.get(id, user.userId())));
    }

    /** Create a new resume from a portfolio. */
    @PostMapping
    public ResponseEntity<ApiResponse<ResumeResponse>> create(
            @RequestBody CreateResumeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.create(user.userId(), request)));
    }

    /** Update resume fields. */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> update(
            @PathVariable Long id,
            @RequestBody UpdateResumeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.update(id, user.userId(), request)));
    }

    /** Delete a resume. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        resumeService.delete(id, user.userId());
        return ResponseEntity.noContent().build();
    }

    /** Get resume preview HTML. */
    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(resumeService.previewHtml(id, user.userId()));
    }

    /** Generate PDF and return download token. */
    @PostMapping("/{id}/pdf")
    public ResponseEntity<ApiResponse<ExportResponse>> generatePdf(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.generatePdf(id, user.userId())));
    }

    /** Return PDF as base64 string for in-app viewing (no download required). */
    @GetMapping("/{id}/pdf/inline")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> inlinePdf(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        String base64 = resumeService.generatePdfBase64(id, user.userId());
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("base64", base64)));
    }

    /** Preview HTML for a specific template (without saving). */
    @GetMapping(value = "/{id}/preview/{templateKey}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewWithTemplate(
            @PathVariable Long id,
            @PathVariable String templateKey,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(resumeService.previewHtmlWithTemplate(id, user.userId(), templateKey));
    }

    /** List available resume templates. */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<TemplateInfo>>> templates() {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.getTemplates()));
    }
}
