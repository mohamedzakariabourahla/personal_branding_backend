package saas.personal_branding.api.auth.infrastructure.mapping;

import java.time.Instant;
import saas.personal_branding.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import saas.personal_branding.api.domain.model.RefreshToken;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

public final class RefreshTokenEntityMapper {

    private RefreshTokenEntityMapper() {
    }

    public static RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
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

    public static void updateEntity(RefreshToken token, RefreshTokenEntity entity, UserEntity userEntity) {
        entity.setUser(userEntity);
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevoked(token.isRevoked());
        entity.setCreatedAt(token.getCreatedAt() != null ? token.getCreatedAt() : Instant.now());
        entity.setDeviceId(token.getDeviceId());
        entity.setDeviceName(token.getDeviceName());
        entity.setUserAgent(token.getUserAgent());
        entity.setIpAddress(token.getIpAddress());
        entity.setLastUsedAt(token.getLastUsedAt());
    }
}
