package com.openfolio.certification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findAllByPortfolioIdOrderByDisplayOrder(Long portfolioId);
}
