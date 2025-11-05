package saas.personal_branding.api.infrastructure.mapping;

import org.junit.jupiter.api.Test;
import saas.personal_branding.api.domain.model.RefreshToken;
import saas.personal_branding.api.infrastructure.persistence.entity.RefreshTokenEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenEntityMapperTest {

    @Test
    void toDomainMapsFields() {
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .id(10L)
                .user(UserEntity.builder().id(20L).build())
                .tokenHash("hash")
                .expiresAt(Instant.parse("2025-02-01T00:00:00Z"))
                .revoked(true)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();

        RefreshToken mapped = RefreshTokenEntityMapper.toDomain(entity);

        assertThat(mapped.getId()).isEqualTo(10L);
        assertThat(mapped.getUserId()).isEqualTo(20L);
        assertThat(mapped.getTokenHash()).isEqualTo("hash");
        assertThat(mapped.isRevoked()).isTrue();
    }

    @Test
    void updateEntityCopiesSource() {
        RefreshToken source = RefreshToken.builder()
                .userId(20L)
                .tokenHash("hash")
                .expiresAt(Instant.parse("2025-02-01T00:00:00Z"))
                .revoked(true)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();

        RefreshTokenEntity target = new RefreshTokenEntity();
        UserEntity user = UserEntity.builder().id(20L).build();

        RefreshTokenEntityMapper.updateEntity(source, target, user);

        assertThat(target.getUser()).isEqualTo(user);
        assertThat(target.getTokenHash()).isEqualTo("hash");
        assertThat(target.getExpiresAt()).isEqualTo(Instant.parse("2025-02-01T00:00:00Z"));
        assertThat(target.isRevoked()).isTrue();
        assertThat(target.getCreatedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
    }
}
