package com.openfolio.portfolio;

import com.openfolio.certification.Certification;
import com.openfolio.certification.CertificationRepository;
import com.openfolio.certification.dto.CertificationRequest;
import com.openfolio.certification.dto.CertificationResponse;
import com.openfolio.education.Education;
import com.openfolio.education.EducationRepository;
import com.openfolio.education.dto.EducationRequest;
import com.openfolio.education.dto.EducationResponse;
import com.openfolio.experience.Experience;
import com.openfolio.experience.ExperienceRepository;
import com.openfolio.experience.dto.ExperienceRequest;
import com.openfolio.experience.dto.ExperienceResponse;
import com.openfolio.shared.exception.ResourceNotFoundException;
import com.openfolio.shared.exception.UnauthorizedException;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for resume-editable portfolio items:
 * Experience, Education, and Certifications.
 */
@RestController
public class ResumeItemsController {

    private final PortfolioRepository portfolioRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final CertificationRepository certificationRepository;

    public ResumeItemsController(PortfolioRepository portfolioRepository,
                                  ExperienceRepository experienceRepository,
                                  EducationRepository educationRepository,
                                  CertificationRepository certificationRepository) {
        this.portfolioRepository = portfolioRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.certificationRepository = certificationRepository;
    }

    // ─── Experience ─────────────────────────────────────────────────────────

    @GetMapping("/api/v1/portfolios/{portfolioId}/experiences")
    public ResponseEntity<ApiResponse<List<ExperienceResponse>>> listExperiences(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        verifyOwnership(portfolioId, user.userId());
        List<ExperienceResponse> list = experienceRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolioId)
                .stream().map(ExperienceResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/api/v1/portfolios/{portfolioId}/experiences")
    @Transactional
    public ResponseEntity<ApiResponse<ExperienceResponse>> createExperience(
            @PathVariable Long portfolioId,
            @RequestBody ExperienceRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Portfolio portfolio = verifyOwnership(portfolioId, user.userId());
        Experience exp = new Experience();
        exp.setPortfolio(portfolio);
        exp.setCompany(req.company());
        exp.setTitle(req.title());
        exp.setDescription(req.description());
        exp.setStartDate(req.startDate());
        exp.setEndDate(req.endDate());
        exp.setCurrent(req.current());
        exp.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : 0);
        experienceRepository.save(exp);
        return ResponseEntity.ok(ApiResponse.ok(ExperienceResponse.from(exp)));
    }

    @PutMapping("/api/v1/experiences/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<ExperienceResponse>> updateExperience(
            @PathVariable Long id,
            @RequestBody ExperienceRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Experience exp = experienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", id));
        verifyOwnership(exp.getPortfolio().getId(), user.userId());
        exp.setCompany(req.company());
        exp.setTitle(req.title());
        exp.setDescription(req.description());
        exp.setStartDate(req.startDate());
        exp.setEndDate(req.endDate());
        exp.setCurrent(req.current());
        if (req.displayOrder() != null) exp.setDisplayOrder(req.displayOrder());
        experienceRepository.save(exp);
        return ResponseEntity.ok(ApiResponse.ok(ExperienceResponse.from(exp)));
    }

    @DeleteMapping("/api/v1/experiences/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteExperience(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Experience exp = experienceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Experience", id));
        verifyOwnership(exp.getPortfolio().getId(), user.userId());
        experienceRepository.delete(exp);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Education ──────────────────────────────────────────────────────────

    @GetMapping("/api/v1/portfolios/{portfolioId}/education")
    public ResponseEntity<ApiResponse<List<EducationResponse>>> listEducation(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        verifyOwnership(portfolioId, user.userId());
        List<EducationResponse> list = educationRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolioId)
                .stream().map(EducationResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/api/v1/portfolios/{portfolioId}/education")
    @Transactional
    public ResponseEntity<ApiResponse<EducationResponse>> createEducation(
            @PathVariable Long portfolioId,
            @RequestBody EducationRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Portfolio portfolio = verifyOwnership(portfolioId, user.userId());
        Education edu = new Education();
        edu.setPortfolio(portfolio);
        edu.setInstitution(req.institution());
        edu.setDegree(req.degree());
        edu.setField(req.field());
        edu.setStartYear(req.startYear());
        edu.setEndYear(req.endYear());
        edu.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : 0);
        educationRepository.save(edu);
        return ResponseEntity.ok(ApiResponse.ok(EducationResponse.from(edu)));
    }

    @PutMapping("/api/v1/education/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<EducationResponse>> updateEducation(
            @PathVariable Long id,
            @RequestBody EducationRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Education edu = educationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education", id));
        verifyOwnership(edu.getPortfolio().getId(), user.userId());
        edu.setInstitution(req.institution());
        edu.setDegree(req.degree());
        edu.setField(req.field());
        edu.setStartYear(req.startYear());
        edu.setEndYear(req.endYear());
        if (req.displayOrder() != null) edu.setDisplayOrder(req.displayOrder());
        educationRepository.save(edu);
        return ResponseEntity.ok(ApiResponse.ok(EducationResponse.from(edu)));
    }

    @DeleteMapping("/api/v1/education/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteEducation(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Education edu = educationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Education", id));
        verifyOwnership(edu.getPortfolio().getId(), user.userId());
        educationRepository.delete(edu);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Certifications ─────────────────────────────────────────────────────

    @GetMapping("/api/v1/portfolios/{portfolioId}/certifications")
    public ResponseEntity<ApiResponse<List<CertificationResponse>>> listCertifications(
            @PathVariable Long portfolioId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        verifyOwnership(portfolioId, user.userId());
        List<CertificationResponse> list = certificationRepository
                .findAllByPortfolioIdOrderByDisplayOrder(portfolioId)
                .stream().map(CertificationResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/api/v1/portfolios/{portfolioId}/certifications")
    @Transactional
    public ResponseEntity<ApiResponse<CertificationResponse>> createCertification(
            @PathVariable Long portfolioId,
            @RequestBody CertificationRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Portfolio portfolio = verifyOwnership(portfolioId, user.userId());
        Certification cert = new Certification();
        cert.setPortfolio(portfolio);
        cert.setName(req.name());
        cert.setIssuingOrganization(req.issuingOrganization());
        cert.setIssueDate(req.issueDate());
        cert.setExpiryDate(req.expiryDate());
        cert.setCredentialId(req.credentialId());
        cert.setCredentialUrl(req.credentialUrl());
        cert.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : 0);
        certificationRepository.save(cert);
        return ResponseEntity.ok(ApiResponse.ok(CertificationResponse.from(cert)));
    }

    @PutMapping("/api/v1/certifications/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<CertificationResponse>> updateCertification(
            @PathVariable Long id,
            @RequestBody CertificationRequest req,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Certification cert = certificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", id));
        verifyOwnership(cert.getPortfolio().getId(), user.userId());
        cert.setName(req.name());
        cert.setIssuingOrganization(req.issuingOrganization());
        cert.setIssueDate(req.issueDate());
        cert.setExpiryDate(req.expiryDate());
        cert.setCredentialId(req.credentialId());
        cert.setCredentialUrl(req.credentialUrl());
        if (req.displayOrder() != null) cert.setDisplayOrder(req.displayOrder());
        certificationRepository.save(cert);
        return ResponseEntity.ok(ApiResponse.ok(CertificationResponse.from(cert)));
    }

    @DeleteMapping("/api/v1/certifications/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteCertification(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        Certification cert = certificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certification", id));
        verifyOwnership(cert.getPortfolio().getId(), user.userId());
        certificationRepository.delete(cert);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Portfolio verifyOwnership(Long portfolioId, Long userId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return portfolio;
    }
}
