package com.openfolio.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthIdentityRepository extends JpaRepository<AuthIdentity, Long> {

    Optional<AuthIdentity> findByProviderAndProviderUid(AuthProvider provider, String providerUid);

    Optional<AuthIdentity> findByUserIdAndProvider(Long userId, AuthProvider provider);
}
