package saas.personal_branding.api.infrastructure.mapping;

import org.junit.jupiter.api.Test;
import saas.personal_branding.api.domain.model.PasswordResetToken;
import saas.personal_branding.api.infrastructure.persistence.entity.PasswordResetTokenEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenEntityMapperTest {

    @Test
    void toDomainCopiesValues() {
        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .id(1L)
                .user(UserEntity.builder().id(2L).build())
                .tokenHash("hash")
                .expiresAt(Instant.parse("2025-03-01T00:00:00Z"))
                .createdAt(Instant.parse("2025-02-01T00:00:00Z"))
                .usedAt(Instant.parse("2025-02-15T00:00:00Z"))
                .build();

        PasswordResetToken token = PasswordResetTokenEntityMapper.toDomain(entity);

        assertThat(token.getId()).isEqualTo(1L);
        assertThat(token.getUserId()).isEqualTo(2L);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(Instant.parse("2025-03-01T00:00:00Z"));
        assertThat(token.getUsedAt()).isEqualTo(Instant.parse("2025-02-15T00:00:00Z"));
    }
}
