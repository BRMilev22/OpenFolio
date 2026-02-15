package com.openfolio.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findAllByPortfolioIdOrderByDisplayOrder(Long portfolioId);
    long countByPortfolioId(Long portfolioId);
}
