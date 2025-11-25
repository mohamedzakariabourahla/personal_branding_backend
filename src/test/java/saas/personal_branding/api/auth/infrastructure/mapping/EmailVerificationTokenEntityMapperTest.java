package saas.personal_branding.api.auth.infrastructure.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import saas.personal_branding.api.auth.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import saas.personal_branding.api.domain.model.EmailVerificationToken;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;

class EmailVerificationTokenEntityMapperTest {

    @Test
    void toDomainCopiesValues() {
        EmailVerificationTokenEntity entity = EmailVerificationTokenEntity.builder()
                .id(11L)
                .user(UserEntity.builder().id(22L).build())
                .tokenHash("hash")
                .expiresAt(Instant.parse("2025-04-01T00:00:00Z"))
                .createdAt(Instant.parse("2025-03-01T00:00:00Z"))
                .usedAt(null)
                .build();

        EmailVerificationToken token = EmailVerificationTokenEntityMapper.toDomain(entity);

        assertThat(token.getId()).isEqualTo(11L);
        assertThat(token.getUserId()).isEqualTo(22L);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getExpiresAt()).isEqualTo(Instant.parse("2025-04-01T00:00:00Z"));
        assertThat(token.getCreatedAt()).isEqualTo(Instant.parse("2025-03-01T00:00:00Z"));
        assertThat(token.getUsedAt()).isNull();
    }
}
