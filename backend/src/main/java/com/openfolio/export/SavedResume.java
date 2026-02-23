package com.openfolio.export;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_resumes")
@Getter @Setter @NoArgsConstructor
public class SavedResume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(nullable = false)
    private String title;

    @Column(name = "template_key", nullable = false, length = 50)
    private String templateKey;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Lob
    @Column(name = "pdf_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] pdfData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Non-null when the resume is publicly accessible. */
    @Column(name = "publish_token", unique = true, length = 64)
    private String publishToken;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isPublished() {
        return publishToken != null;
    }

    public SavedResume(Long userId, Long portfolioId, String title,
                       String templateKey, byte[] pdfData) {
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.title = title;
        this.templateKey = templateKey;
        this.pdfData = pdfData;
        this.fileSizeBytes = pdfData.length;
    }
}
