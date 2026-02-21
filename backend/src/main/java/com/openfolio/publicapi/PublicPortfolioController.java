package com.openfolio.publicapi;

import com.openfolio.portfolio.PortfolioBundle;
import com.openfolio.portfolio.PortfolioDataLoader;
import com.openfolio.portfolio.PortfolioHtmlGenerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Unauthenticated endpoints for published portfolios.
 * Anyone with the portfolio slug can access these.
 */
@RestController
@RequestMapping("/api/v1/public")
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
    public String getPublicPortfolio(@PathVariable String slug) {
        PortfolioBundle bundle = dataLoader.loadBySlug(slug);
        return htmlGenerator.generate(bundle);
    }

    /** Returns JSON metadata for a published portfolio. */
    @GetMapping(value = "/{slug}/meta", produces = MediaType.APPLICATION_JSON_VALUE)
    public PublicPortfolioMeta getPublicMeta(@PathVariable String slug) {
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
