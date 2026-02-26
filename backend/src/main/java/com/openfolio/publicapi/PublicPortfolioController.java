package com.openfolio.publicapi;

import com.openfolio.portfolio.PortfolioBundle;
import com.openfolio.portfolio.PortfolioDataLoader;
import com.openfolio.portfolio.PortfolioHtmlGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Unauthenticated endpoints for published portfolios.
 * Anyone with the portfolio slug can access these.
 */
@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public", description = "Unauthenticated endpoints for published portfolios and shared resumes")
public class PublicPortfolioController {

    private final PortfolioDataLoader dataLoader;
    private final PortfolioHtmlGenerator htmlGenerator;

    public PublicPortfolioController(PortfolioDataLoader dataLoader,
                                     PortfolioHtmlGenerator htmlGenerator) {
        this.dataLoader = dataLoader;
        this.htmlGenerator = htmlGenerator;
    }

    /** Returns the rendered portfolio HTML for a published portfolio by slug. */
    @GetMapping(value = "/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Get public portfolio", description = "Returns the full rendered HTML page for a published portfolio.", security = {})
    public String getPublicPortfolio(@Parameter(description = "Portfolio slug", example = "john-doe-a1b2") @PathVariable String slug) {
        PortfolioBundle bundle = dataLoader.loadBySlug(slug);
        return htmlGenerator.generate(bundle);
    }

    /** Returns JSON metadata for a published portfolio. */
    @GetMapping(value = "/{slug}/meta", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get public portfolio metadata", description = "Returns JSON metadata (title, tagline, counts) for Open Graph tags and social previews.", security = {})
    public PublicPortfolioMeta getPublicMeta(@Parameter(description = "Portfolio slug", example = "john-doe-a1b2") @PathVariable String slug) {
        PortfolioBundle bundle = dataLoader.loadBySlug(slug);
        return new PublicPortfolioMeta(
                bundle.portfolio().getSlug(),
                bundle.portfolio().getTitle(),
                bundle.portfolio().getTagline(),
                bundle.user() != null ? bundle.user().getDisplayName() : null,
                bundle.projects().size(),
                bundle.skills().size()
        );
    }

    public record PublicPortfolioMeta(
            String slug, String title, String tagline,
            String displayName, int projectCount, int skillCount) {}
}
