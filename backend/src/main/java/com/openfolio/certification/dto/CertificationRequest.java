package com.openfolio.certification.dto;

import java.time.LocalDate;

public record CertificationRequest(
        String name,
        String issuingOrganization,
        LocalDate issueDate,
        LocalDate expiryDate,
        String credentialId,
        String credentialUrl,
        Integer displayOrder) {}
