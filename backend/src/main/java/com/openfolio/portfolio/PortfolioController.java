package com.openfolio.portfolio;

import com.openfolio.portfolio.dto.CreatePortfolioRequest;
import com.openfolio.portfolio.dto.PortfolioSummaryResponse;
import com.openfolio.portfolio.dto.UpdatePortfolioRequest;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public ApiResponse<List<PortfolioSummaryResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(portfolioService.getPortfolios(user.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PortfolioSummaryResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreatePortfolioRequest request) {
        return ApiResponse.ok(portfolioService.create(user.userId(), request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<PortfolioSummaryResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        return ApiResponse.ok(portfolioService.update(id, user.userId(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        portfolioService.delete(id, user.userId());
    }
}
