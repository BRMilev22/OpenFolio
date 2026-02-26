package com.openfolio.publish;

import com.openfolio.publish.dto.PublishResponse;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Publishing", description = "Publish and unpublish portfolios to get a shareable public URL")
public class PublishController {

    private final PublishService publishService;

    public PublishController(PublishService publishService) {
        this.publishService = publishService;
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish portfolio", description = "Assigns a public slug and makes the portfolio accessible at /api/v1/public/{slug}.")
    public ResponseEntity<ApiResponse<PublishResponse>> publish(@PathVariable Long id,
                                                                @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(publishService.publish(id, user.userId())));
    }

    @DeleteMapping("/{id}/publish")
    @Operation(summary = "Unpublish portfolio", description = "Deactivates the public URL. Content is preserved.")
    public ResponseEntity<Void> unpublish(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        publishService.unpublish(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
