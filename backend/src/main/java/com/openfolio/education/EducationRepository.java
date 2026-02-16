package com.openfolio.education;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findAllByPortfolioIdOrderByDisplayOrder(Long portfolioId);
}
