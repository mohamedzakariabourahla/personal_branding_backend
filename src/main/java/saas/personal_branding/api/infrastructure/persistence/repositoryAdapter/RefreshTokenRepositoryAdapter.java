package saas.personal_branding.api.infrastructure.persistence.repositoryAdapter;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.personal_branding.api.domain.model.RefreshToken;
import saas.personal_branding.api.domain.repository.RefreshTokenRepository;
import saas.personal_branding.api.infrastructure.mapping.RefreshTokenEntityMapper;
import saas.personal_branding.api.infrastructure.persistence.entity.RefreshTokenEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaRefreshTokenRepository;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpaRefreshTokenRepository;
    private final JpaUserRepository jpaUserRepository;

    public RefreshTokenRepositoryAdapter(JpaRefreshTokenRepository jpaRefreshTokenRepository,
                                         JpaUserRepository jpaUserRepository) {
        this.jpaRefreshTokenRepository = jpaRefreshTokenRepository;
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        UserEntity userEntity = jpaUserRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + token.getUserId()));

        RefreshTokenEntity entity = token.getId() != null
                ? jpaRefreshTokenRepository.findById(token.getId()).orElse(new RefreshTokenEntity())
                : new RefreshTokenEntity();

        RefreshTokenEntityMapper.updateEntity(token, entity, userEntity);

        RefreshTokenEntity saved = jpaRefreshTokenRepository.save(entity);
        return RefreshTokenEntityMapper.toDomain(saved);
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        jpaRefreshTokenRepository.revokeAllByUserId(userId);
    }

    @Override
    public void revokeById(Long tokenId) {
        jpaRefreshTokenRepository.revokeById(tokenId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findActiveByTokenHash(String tokenHash) {
        return jpaRefreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .map(RefreshTokenEntityMapper::toDomain);
    }

    @Override
    public int revokeByUserIdAndDeviceId(Long userId, String deviceId) {
        if (deviceId == null) {
            return 0;
        }
        return jpaRefreshTokenRepository.revokeByUserIdAndDeviceId(userId, deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findActiveByUserId(Long userId) {
        return jpaRefreshTokenRepository.findActiveByUserIdOrderByLastUsedAtDesc(userId)
                .stream()
                .map(RefreshTokenEntityMapper::toDomain)
                .toList();
    }
}

