package com.openfolio.publish;

import com.openfolio.portfolio.Portfolio;
import com.openfolio.portfolio.PortfolioRepository;
import com.openfolio.publish.dto.PublishResponse;
import com.openfolio.shared.exception.ResourceNotFoundException;
import com.openfolio.shared.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PublishService {

    private final PortfolioRepository portfolioRepository;
    private final PublishRecordRepository publishRecordRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public PublishService(PortfolioRepository portfolioRepository,
                          PublishRecordRepository publishRecordRepository) {
        this.portfolioRepository = portfolioRepository;
        this.publishRecordRepository = publishRecordRepository;
    }

    @Transactional
    public PublishResponse publish(Long portfolioId, Long userId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }

        portfolio.setPublished(true);
        portfolioRepository.save(portfolio);

        // Determine version number
        int version = publishRecordRepository
                .findTopByPortfolioIdOrderByPublishedAtDesc(portfolioId)
                .map(r -> r.getVersion() + 1)
                .orElse(1);

        String publicUrl = baseUrl + "/api/v1/public/" + portfolio.getSlug();

        PublishRecord record = new PublishRecord();
        record.setPortfolio(portfolio);
        record.setPublishedUrl(publicUrl);
        record.setVersion(version);
        record.setPublishedAt(LocalDateTime.now());
        publishRecordRepository.save(record);

        return new PublishResponse(portfolioId, portfolio.getSlug(), publicUrl, version, record.getPublishedAt());
    }

    @Transactional
    public void unpublish(Long portfolioId, Long userId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", portfolioId));
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        portfolio.setPublished(false);
        portfolioRepository.save(portfolio);
    }
}
