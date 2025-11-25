package saas.personal_branding.api.auth.infrastructure.mapping;

import java.time.Instant;
import saas.personal_branding.api.auth.infrastructure.persistence.entity.PasswordResetTokenEntity;
import saas.personal_branding.api.domain.model.PasswordResetToken;

public final class PasswordResetTokenEntityMapper {

    private PasswordResetTokenEntityMapper() {
    }

    public static PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return PasswordResetToken.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .usedAt(entity.getUsedAt())
                .build();
    }

    public static void updateEntity(PasswordResetToken token, PasswordResetTokenEntity entity, saas.personal_branding.api.infrastructure.persistence.entity.UserEntity user) {
        entity.setUser(user);
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setCreatedAt(token.getCreatedAt() != null ? token.getCreatedAt() : Instant.now());
        entity.setUsedAt(token.getUsedAt());
    }
}
