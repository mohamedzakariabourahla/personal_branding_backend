package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.EmailVerificationToken;
import saas.personal_branding.api.domain.repository.EmailVerificationTokenRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaEmailVerificationTokenRepository;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

import java.time.Instant;
import java.util.Optional;

@Repository
@Transactional
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {

    private final JpaEmailVerificationTokenRepository jpaRepository;
    private final JpaUserRepository jpaUserRepository;

    public EmailVerificationTokenRepositoryAdapter(JpaEmailVerificationTokenRepository jpaRepository,
                                                   JpaUserRepository jpaUserRepository) {
        this.jpaRepository = jpaRepository;
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        UserEntity userEntity = jpaUserRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + token.getUserId()));

        EmailVerificationTokenEntity entity = token.getId() != null
                ? jpaRepository.findById(token.getId()).orElse(new EmailVerificationTokenEntity())
                : new EmailVerificationTokenEntity();

        entity.setUser(userEntity);
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setCreatedAt(token.getCreatedAt() != null ? token.getCreatedAt() : Instant.now());
        entity.setUsedAt(token.getUsedAt());

        EmailVerificationTokenEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(this::toDomain);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteAllByUserId(userId);
    }

    @Override
    public void markUsed(Long tokenId) {
        jpaRepository.markUsed(tokenId, Instant.now());
    }

    private EmailVerificationToken toDomain(EmailVerificationTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return EmailVerificationToken.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .usedAt(entity.getUsedAt())
                .build();
    }
}
