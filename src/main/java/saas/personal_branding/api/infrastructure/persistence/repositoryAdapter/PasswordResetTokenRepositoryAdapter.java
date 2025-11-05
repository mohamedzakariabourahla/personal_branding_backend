package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.PasswordResetToken;
import saas.personal_branding.api.domain.repository.PasswordResetTokenRepository;
import saas.personal_branding.api.infrastructure.persistence.entity.PasswordResetTokenEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaPasswordResetTokenRepository;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

import java.time.Instant;
import java.util.Optional;

@Repository
@Transactional
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final JpaPasswordResetTokenRepository jpaPasswordResetTokenRepository;
    private final JpaUserRepository jpaUserRepository;

    public PasswordResetTokenRepositoryAdapter(JpaPasswordResetTokenRepository jpaPasswordResetTokenRepository,
                                               JpaUserRepository jpaUserRepository) {
        this.jpaPasswordResetTokenRepository = jpaPasswordResetTokenRepository;
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        UserEntity userEntity = jpaUserRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + token.getUserId()));

        PasswordResetTokenEntity entity = token.getId() != null
                ? jpaPasswordResetTokenRepository.findById(token.getId()).orElse(new PasswordResetTokenEntity())
                : new PasswordResetTokenEntity();

        entity.setUser(userEntity);
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setCreatedAt(token.getCreatedAt() != null ? token.getCreatedAt() : Instant.now());
        entity.setUsedAt(token.getUsedAt());

        PasswordResetTokenEntity saved = jpaPasswordResetTokenRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaPasswordResetTokenRepository.findByTokenHash(tokenHash)
                .map(this::toDomain);
    }

    @Override
    public void markUsed(Long tokenId) {
        jpaPasswordResetTokenRepository.markUsed(tokenId, Instant.now());
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaPasswordResetTokenRepository.deleteAllByUserId(userId);
    }

    private PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return PasswordResetToken.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .usedAt(entity.getUsedAt())
                .build();
    }
}
