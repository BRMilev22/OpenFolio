package com.openfolio.publish;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublishRecordRepository extends JpaRepository<PublishRecord, Long> {
    Optional<PublishRecord> findTopByPortfolioIdOrderByPublishedAtDesc(Long portfolioId);
    List<PublishRecord> findAllByPortfolioId(Long portfolioId);
}
