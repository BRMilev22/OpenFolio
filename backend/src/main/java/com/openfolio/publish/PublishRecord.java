package com.openfolio.publish;

import com.openfolio.portfolio.Portfolio;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "publish_records")
@Getter
@Setter
@NoArgsConstructor
public class PublishRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(name = "published_url", nullable = false, columnDefinition = "TEXT")
    private String publishedUrl;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;
}
