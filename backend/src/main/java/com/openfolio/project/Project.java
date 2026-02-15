package com.openfolio.project;

import com.openfolio.portfolio.Portfolio;
import com.openfolio.shared.persistence.StringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(name = "github_repo_id")
    private String githubRepoId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ai_enhanced_description", columnDefinition = "TEXT")
    private String aiEnhancedDescription;

    @Column(name = "ai_enhanced_at")
    private LocalDateTime aiEnhancedAt;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Convert(converter = StringListConverter.class)
    @Column(name = "languages", columnDefinition = "TEXT")
    private List<String> languages = new ArrayList<>();

    private int stars;
    private int forks;

    @Column(name = "is_highlighted")
    private boolean highlighted;

    @Column(name = "display_order")
    private int displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
