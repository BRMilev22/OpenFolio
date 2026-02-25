package com.openfolio.certification.dto;

import com.openfolio.certification.Certification;

import java.time.LocalDate;

public record CertificationResponse(
        Long id,
        String name,
        String issuingOrganization,
        LocalDate issueDate,
        LocalDate expiryDate,
        String credentialId,
        String credentialUrl,
        int displayOrder) {

    public static CertificationResponse from(Certification c) {
        return new CertificationResponse(
                c.getId(), c.getName(), c.getIssuingOrganization(),
                c.getIssueDate(), c.getExpiryDate(),
                c.getCredentialId(), c.getCredentialUrl(), c.getDisplayOrder());
    }
}
