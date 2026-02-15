package com.openfolio.portfolio;

import com.openfolio.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String slug;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String tagline;

    @Column(name = "ai_enhanced_summary", columnDefinition = "TEXT")
    private String aiEnhancedSummary;

    @Column(name = "ai_enhanced_at")
    private LocalDateTime aiEnhancedAt;

    @Column(name = "is_published")
    private boolean published;

    @Column(name = "theme_key")
    private String themeKey = "dark";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
