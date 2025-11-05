package saas.personal_branding.api.infrastructure.mapping;

import saas.personal_branding.api.domain.model.RefreshToken;
import saas.personal_branding.api.infrastructure.persistence.entity.RefreshTokenEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

import java.time.Instant;

public final class RefreshTokenEntityMapper {

    private RefreshTokenEntityMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .revoked(entity.isRevoked())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static void updateEntity(RefreshToken source, RefreshTokenEntity target, UserEntity userEntity) {
        target.setUser(userEntity);
        target.setTokenHash(source.getTokenHash());
        target.setExpiresAt(source.getExpiresAt());
        target.setRevoked(source.isRevoked());
        target.setCreatedAt(source.getCreatedAt() != null ? source.getCreatedAt() : Instant.now());
    }
}
