package saas.personal_branding.api.infrastructure.mapping;

import saas.personal_branding.api.domain.model.PasswordResetToken;
import saas.personal_branding.api.infrastructure.persistence.entity.PasswordResetTokenEntity;

public final class PasswordResetTokenEntityMapper {

    private PasswordResetTokenEntityMapper() {
    }

    public static PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
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
