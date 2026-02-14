package com.openfolio.auth;

import com.openfolio.auth.dto.*;
import com.openfolio.auth.oauth.OAuthProfile;
import com.openfolio.auth.oauth.OAuthProvider;
import com.openfolio.shared.config.JwtConfig;
import com.openfolio.shared.exception.ConflictException;
import com.openfolio.shared.exception.UnauthorizedException;
import com.openfolio.shared.security.JwtTokenProvider;
import com.openfolio.user.User;
import com.openfolio.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthIdentityRepository identityRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final Map<String, OAuthProvider> oauthProviders;

    public AuthService(UserRepository userRepository,
                       AuthIdentityRepository identityRepository,
                       JwtTokenProvider tokenProvider,
                       PasswordEncoder passwordEncoder,
                       JwtConfig jwtConfig,
                       Map<String, OAuthProvider> oauthProviders) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.oauthProviders = oauthProviders;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already registered");
        }
        User user = userRepository.save(new User(request.email(), request.displayName()));
        AuthIdentity identity = new AuthIdentity(user, AuthProvider.EMAIL, request.email());
        identity.setAccessToken(passwordEncoder.encode(request.password()));
        identityRepository.save(identity);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        AuthIdentity identity = identityRepository
                .findByUserIdAndProvider(user.getId(), AuthProvider.EMAIL)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), identity.getAccessToken())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse handleOAuth(String providerKey, OAuthCallbackRequest request) {
        OAuthProvider provider = resolveProvider(providerKey);
        String accessToken = provider.exchangeCodeForToken(request.code(), request.redirectUri());
        OAuthProfile profile = provider.fetchProfile(accessToken);
        AuthProvider authProvider = AuthProvider.valueOf(providerKey.toUpperCase());

        return identityRepository
                .findByProviderAndProviderUid(authProvider, profile.providerUid())
                .map(identity -> {
                    identity.setAccessToken(accessToken);
                    identityRepository.save(identity);
                    User existing = identity.getUser();
                    updateUserFromProfile(existing, profile, authProvider);
                    return issueTokens(existing);
                })
                .orElseGet(() -> {
                    String email = profile.email() != null ? profile.email()
                            : profile.login() + "@users.noreply.github.com";
                    String name = profile.displayName() != null ? profile.displayName()
                            : profile.login();
                    User user = userRepository.findByEmail(email)
                            .orElseGet(() -> userRepository.save(new User(email, name)));
                    updateUserFromProfile(user, profile, authProvider);
                    AuthIdentity identity = new AuthIdentity(user, authProvider, profile.providerUid());
                    identity.setAccessToken(accessToken);
                    identityRepository.save(identity);
                    return issueTokens(user);
                });
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        if (!tokenProvider.isValid(request.refreshToken())) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        var claims = tokenProvider.parseToken(request.refreshToken());
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new UnauthorizedException("Invalid token type");
        }
        return issueTokens(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class));
    }

    private TokenResponse issueTokens(User user) {
        return new TokenResponse(
                tokenProvider.generateAccessToken(user.getId(), user.getEmail()),
                tokenProvider.generateRefreshToken(user.getId(), user.getEmail()),
                jwtConfig.accessTokenExpiryMs(),
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getGithubUsername());
    }

    private TokenResponse issueTokens(Long userId, String email) {
        return new TokenResponse(
                tokenProvider.generateAccessToken(userId, email),
                tokenProvider.generateRefreshToken(userId, email),
                jwtConfig.accessTokenExpiryMs(),
                userId,
                email,
                null,
                null);
    }

    private void updateUserFromProfile(User user, OAuthProfile profile, AuthProvider provider) {
        if (provider == AuthProvider.GITHUB && profile.login() != null) {
            user.setGithubUsername(profile.login());
        }
        if (profile.avatarUrl() != null) {
            user.setAvatarUrl(profile.avatarUrl());
        }
        if (profile.displayName() != null && (user.getDisplayName() == null || user.getDisplayName().isBlank())) {
            user.setDisplayName(profile.displayName());
        }
        userRepository.save(user);
    }

    private OAuthProvider resolveProvider(String key) {
        OAuthProvider provider = oauthProviders.get(key.toLowerCase() + "OAuthProvider");
        if (provider == null) throw new IllegalArgumentException("Unknown OAuth provider: " + key);
        return provider;
    }
}
