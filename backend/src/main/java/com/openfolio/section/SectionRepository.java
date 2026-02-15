package com.openfolio.section;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findAllByPortfolioIdOrderByDisplayOrder(Long portfolioId);
    Optional<Section> findByPortfolioIdAndType(Long portfolioId, SectionType type);
}
