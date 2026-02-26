package com.openfolio.user;

import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import com.openfolio.user.dto.UpdateUserRequest;
import com.openfolio.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Current user profile management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the profile of the authenticated user.")
    ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(user.userId())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user", description = "Updates display name and/or avatar URL.")
    ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(user.userId(), request)));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user", description = "Permanently deletes the user account and all associated data.")
    ResponseEntity<Void> deleteMe(@AuthenticationPrincipal AuthenticatedUser user) {
        userService.delete(user.userId());
        return ResponseEntity.noContent().build();
    }
}
