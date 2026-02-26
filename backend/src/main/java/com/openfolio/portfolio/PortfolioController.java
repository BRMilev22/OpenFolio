package com.openfolio.portfolio;

import com.openfolio.portfolio.dto.CreatePortfolioRequest;
import com.openfolio.portfolio.dto.PortfolioSummaryResponse;
import com.openfolio.portfolio.dto.UpdatePortfolioRequest;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Portfolios", description = "Portfolio CRUD operations")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    @Operation(summary = "List portfolios", description = "Returns all portfolios belonging to the authenticated user.")
    public ApiResponse<List<PortfolioSummaryResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(portfolioService.getPortfolios(user.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create portfolio", description = "Creates a new empty portfolio for the authenticated user.")
    public ApiResponse<PortfolioSummaryResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreatePortfolioRequest request) {
        return ApiResponse.ok(portfolioService.create(user.userId(), request));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update portfolio", description = "Partially updates portfolio metadata (title, tagline, theme).")
    public ApiResponse<PortfolioSummaryResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdatePortfolioRequest request) {
        return ApiResponse.ok(portfolioService.update(id, user.userId(), request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete portfolio", description = "Permanently deletes a portfolio and all its items.")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        portfolioService.delete(id, user.userId());
    }
}
