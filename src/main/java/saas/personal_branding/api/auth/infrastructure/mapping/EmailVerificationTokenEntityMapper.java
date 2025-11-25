package saas.personal_branding.api.auth.infrastructure.mapping;

import saas.personal_branding.api.auth.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import saas.personal_branding.api.domain.model.EmailVerificationToken;

public final class EmailVerificationTokenEntityMapper {

    private EmailVerificationTokenEntityMapper() {
    }

    public static EmailVerificationToken toDomain(EmailVerificationTokenEntity entity) {
        return EmailVerificationToken.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .usedAt(entity.getUsedAt())
                .build();
    }
}
