package saas.personal_branding.api.domain.repository;

import saas.personal_branding.api.domain.model.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void markUsed(Long tokenId);
    void deleteByUserId(Long userId);
}
