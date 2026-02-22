package com.openfolio.resume;

import com.openfolio.portfolio.Portfolio;
import com.openfolio.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(nullable = false)
    private String title = "My Resume";

    @Column(name = "template_key", nullable = false)
    private String templateKey = "classic";

    // ── header overrides (nullable → fall back to user/portfolio) ──

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "job_title")
    private String jobTitle;

    private String email;
    private String phone;
    private String location;
    private String website;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(columnDefinition = "TEXT")
    private String summary;

    // ── selected item IDs (JSON strings) ──

    @Column(name = "selected_project_ids", columnDefinition = "JSON")
    private String selectedProjectIds;

    @Column(name = "selected_skill_ids", columnDefinition = "JSON")
    private String selectedSkillIds;

    @Column(name = "selected_experience_ids", columnDefinition = "JSON")
    private String selectedExperienceIds;

    @Column(name = "selected_education_ids", columnDefinition = "JSON")
    private String selectedEducationIds;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
