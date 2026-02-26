package com.openfolio.ingestion;

import com.openfolio.ingestion.dto.IngestionRequest;
import com.openfolio.portfolio.dto.PortfolioSummaryResponse;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingestion")
@Tag(name = "GitHub Ingestion", description = "Import GitHub profile and repositories into a portfolio")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/github")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import from GitHub", description = "Fetches the user's GitHub profile, repos, languages, and README. Creates or updates the portfolio, then triggers async AI enhancement.")
    public ApiResponse<PortfolioSummaryResponse> ingestFromGitHub(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody IngestionRequest request) {
        return ApiResponse.ok(ingestionService.ingestFromGitHub(user.userId(), request));
    }
}
