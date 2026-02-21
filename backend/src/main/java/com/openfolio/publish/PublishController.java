package com.openfolio.publish;

import com.openfolio.publish.dto.PublishResponse;
import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolios")
public class PublishController {

    private final PublishService publishService;

    public PublishController(PublishService publishService) {
        this.publishService = publishService;
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<PublishResponse>> publish(@PathVariable Long id,
                                                                @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(publishService.publish(id, user.userId())));
    }

    @DeleteMapping("/{id}/publish")
    public ResponseEntity<Void> unpublish(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthenticatedUser user) {
        publishService.unpublish(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
