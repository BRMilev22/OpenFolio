package com.openfolio.auth;

import com.openfolio.auth.dto.*;
import com.openfolio.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, OAuth callbacks, and JWT token management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register with email and password", description = "Creates a new user account and returns a JWT token pair.", security = {})
    ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password", description = "Authenticates credentials and returns a JWT token pair.", security = {})
    ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT tokens", description = "Exchanges a valid refresh token for a new access + refresh token pair. Old refresh token is invalidated (rotation).", security = {})
    ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidates the current session. Client should discard tokens.")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth/{provider}")
    @Operation(summary = "OAuth callback", description = "Exchanges an OAuth authorization code for JWT tokens. Supported providers: github, linkedin.", security = {})
    ResponseEntity<ApiResponse<TokenResponse>> oauthCallback(
            @Parameter(description = "OAuth provider name", example = "github") @PathVariable String provider,
            @Valid @RequestBody OAuthCallbackRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.handleOAuth(provider, request)));
    }
}
