package com.openfolio.export;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedResumeRepository extends JpaRepository<SavedResume, Long> {
    List<SavedResume> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<SavedResume> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
    void deleteByIdAndUserId(Long id, Long userId);

    Optional<SavedResume> findByPublishToken(String publishToken);
}
