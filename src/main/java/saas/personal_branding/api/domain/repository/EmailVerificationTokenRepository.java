package saas.personal_branding.api.domain.repository;

import saas.personal_branding.api.domain.model.EmailVerificationToken;

import java.util.Optional;

public interface EmailVerificationTokenRepository {
    EmailVerificationToken save(EmailVerificationToken token);
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    void deleteByUserId(Long userId);
    void markUsed(Long tokenId);
}
