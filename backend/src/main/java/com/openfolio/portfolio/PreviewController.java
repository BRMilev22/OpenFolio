package com.openfolio.portfolio;

import com.openfolio.shared.security.AuthenticatedUser;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PreviewController {

    private final PortfolioDataLoader dataLoader;
    private final PortfolioHtmlGenerator htmlGenerator;

    public PreviewController(PortfolioDataLoader dataLoader, PortfolioHtmlGenerator htmlGenerator) {
        this.dataLoader = dataLoader;
        this.htmlGenerator = htmlGenerator;
    }

    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String preview(@PathVariable Long id,
                          @AuthenticationPrincipal AuthenticatedUser user) {
        PortfolioBundle bundle = dataLoader.load(id, user.userId());
        return htmlGenerator.generate(bundle);
    }
}
