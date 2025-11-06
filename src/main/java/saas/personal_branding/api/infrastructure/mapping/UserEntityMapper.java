package saas.personal_branding.api.infrastructure.mapping;

import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.domain.util.EmailNormalizer;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

import java.util.HashSet;

public final class UserEntityMapper {

    private UserEntityMapper() {
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.builder()
                .id(entity.getId())
                .email(EmailNormalizer.normalize(entity.getEmail()))
                .passwordHash(entity.getPasswordHash())
                .active(Boolean.TRUE.equals(entity.getActive()))
                .emailVerified(Boolean.TRUE.equals(entity.getEmailVerified()))
                .emailVerifiedAt(entity.getEmailVerifiedAt())
                .onboardingStatus(entity.getOnboardingStatus())
                .roles(entity.getRoles() == null ? java.util.Set.of() : java.util.Set.copyOf(entity.getRoles()))
                .person(PersonEntityMapper.toDomain(entity.getPerson()))
                .build();
    }

    public static void updateEntity(User source, UserEntity target) {
        target.setEmail(EmailNormalizer.normalize(source.getEmail()));
        target.setPasswordHash(source.getPasswordHash());
        target.setActive(source.isActive());
        target.setOnboardingStatus(source.getOnboardingStatus());
        target.setEmailVerified(source.isEmailVerified());
        target.setEmailVerifiedAt(source.getEmailVerifiedAt());
        target.setRoles(source.getRoles().isEmpty() ? new HashSet<>() : new HashSet<>(source.getRoles()));
    }
}
