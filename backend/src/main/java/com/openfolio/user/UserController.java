package com.openfolio.user;

import com.openfolio.shared.security.AuthenticatedUser;
import com.openfolio.shared.web.ApiResponse;
import com.openfolio.user.dto.UpdateUserRequest;
import com.openfolio.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(user.userId())));
    }

    @PutMapping("/me")
    ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(user.userId(), request)));
    }

    @DeleteMapping("/me")
    ResponseEntity<Void> deleteMe(@AuthenticationPrincipal AuthenticatedUser user) {
        userService.delete(user.userId());
        return ResponseEntity.noContent().build();
    }
}
