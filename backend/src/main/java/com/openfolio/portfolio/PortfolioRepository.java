package com.openfolio.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Portfolio> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
