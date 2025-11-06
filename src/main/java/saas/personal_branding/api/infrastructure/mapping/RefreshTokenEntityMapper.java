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
                .deviceId(entity.getDeviceId())
                .deviceName(entity.getDeviceName())
                .userAgent(entity.getUserAgent())
                .ipAddress(entity.getIpAddress())
                .lastUsedAt(entity.getLastUsedAt())
                .build();
    }

    public static void updateEntity(RefreshToken source, RefreshTokenEntity target, UserEntity userEntity) {
        target.setUser(userEntity);
        target.setTokenHash(source.getTokenHash());
        target.setExpiresAt(source.getExpiresAt());
        target.setRevoked(source.isRevoked());
        Instant createdAt = source.getCreatedAt() != null ? source.getCreatedAt() : Instant.now();
        target.setCreatedAt(createdAt);
        target.setDeviceId(source.getDeviceId());
        target.setDeviceName(source.getDeviceName());
        target.setUserAgent(source.getUserAgent());
        target.setIpAddress(source.getIpAddress());
        target.setLastUsedAt(source.getLastUsedAt() != null ? source.getLastUsedAt() : createdAt);
    }
}
