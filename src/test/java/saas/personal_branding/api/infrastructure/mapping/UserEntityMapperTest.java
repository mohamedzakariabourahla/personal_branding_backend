package saas.personal_branding.api.infrastructure.mapping;

import org.junit.jupiter.api.Test;
import saas.personal_branding.api.domain.model.Role;
import saas.personal_branding.api.domain.model.User;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityMapperTest {

    @Test
    void toDomainReturnsNullWhenEntityNull() {
        assertThat(UserEntityMapper.toDomain(null)).isNull();
    }

    @Test
    void toDomainCopiesPrimitiveFields() {
        UserEntity entity = UserEntity.builder()
                .id(42L)
                .email("user@example.com")
                .passwordHash("hash")
                .active(true)
                .emailVerified(true)
                .emailVerifiedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .onboardingStatus(saas.personal_branding.api.domain.model.OnboardingStatus.PROFILE_PENDING)
                .roles(Set.of(Role.ADMIN))
                .build();

        User mapped = UserEntityMapper.toDomain(entity);

        assertThat(mapped.getId()).isEqualTo(42L);
        assertThat(mapped.getEmail()).isEqualTo("user@example.com");
        assertThat(mapped.getPasswordHash()).isEqualTo("hash");
        assertThat(mapped.isActive()).isTrue();
        assertThat(mapped.isEmailVerified()).isTrue();
        assertThat(mapped.getEmailVerifiedAt()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
        assertThat(mapped.getRoles()).containsExactly(Role.ADMIN);
    }

    @Test
    void updateEntityCopiesValuesFromDomain() {
        User source = User.builder()
                .email("new@example.com")
                .passwordHash("new-hash")
                .active(false)
                .emailVerified(true)
                .emailVerifiedAt(Instant.parse("2025-01-01T00:00:00Z"))
                .onboardingStatus(saas.personal_branding.api.domain.model.OnboardingStatus.COMPLETED)
                .roles(Set.of(Role.CLIENT))
                .build();

        UserEntity target = new UserEntity();

        UserEntityMapper.updateEntity(source, target);

        assertThat(target.getEmail()).isEqualTo("new@example.com");
        assertThat(target.getPasswordHash()).isEqualTo("new-hash");
        assertThat(target.getActive()).isFalse();
        assertThat(target.getEmailVerified()).isTrue();
        assertThat(target.getEmailVerifiedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
        assertThat(target.getOnboardingStatus()).isEqualTo(saas.personal_branding.api.domain.model.OnboardingStatus.COMPLETED);
        assertThat(target.getRoles()).containsExactly(Role.CLIENT);
    }
}
