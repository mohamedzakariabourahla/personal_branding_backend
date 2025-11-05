package saas.personal_branding.api.infrastructure.mapping;

import saas.personal_branding.api.domain.model.EmailVerificationToken;
import saas.personal_branding.api.infrastructure.persistence.entity.EmailVerificationTokenEntity;

public final class EmailVerificationTokenEntityMapper {

    private EmailVerificationTokenEntityMapper() {
    }

    public static EmailVerificationToken toDomain(EmailVerificationTokenEntity entity) {
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
