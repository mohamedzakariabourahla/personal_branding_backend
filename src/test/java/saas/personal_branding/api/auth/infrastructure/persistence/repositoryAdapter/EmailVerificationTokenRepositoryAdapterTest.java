package saas.personal_branding.api.auth.infrastructure.persistence.repositoryAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.personal_branding.api.auth.infrastructure.persistence.entity.EmailVerificationTokenEntity;
import saas.personal_branding.api.auth.infrastructure.persistence.jpa.JpaEmailVerificationTokenRepository;
import saas.personal_branding.api.domain.model.EmailVerificationToken;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenRepositoryAdapterTest {

    @Mock
    private JpaEmailVerificationTokenRepository jpaRepository;
    @Mock
    private JpaUserRepository jpaUserRepository;

    private EmailVerificationTokenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EmailVerificationTokenRepositoryAdapter(jpaRepository, jpaUserRepository);
    }

    @Test
    void savePersistsAndMaps() {
        UserEntity user = UserEntity.builder().id(7L).email("user@example.com").build();
        when(jpaUserRepository.findById(7L)).thenReturn(Optional.of(user));

        EmailVerificationTokenEntity saved = EmailVerificationTokenEntity.builder()
                .id(33L)
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.parse("2025-02-01T00:00:00Z"))
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();
        when(jpaRepository.save(any())).thenReturn(saved);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(7L)
                .tokenHash("hash")
                .expiresAt(saved.getExpiresAt())
                .createdAt(saved.getCreatedAt())
                .build();

        EmailVerificationToken result = adapter.save(token);

        assertThat(result.getId()).isEqualTo(33L);
        assertThat(result.getUserId()).isEqualTo(7L);
    }

    @Test
    void findByTokenHashMaps() {
        UserEntity user = UserEntity.builder().id(8L).email("user@example.com").build();
        EmailVerificationTokenEntity entity = EmailVerificationTokenEntity.builder()
                .id(44L)
                .user(user)
                .tokenHash("hash2")
                .expiresAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        when(jpaRepository.findByTokenHash("hash2")).thenReturn(Optional.of(entity));

        Optional<EmailVerificationToken> result = adapter.findByTokenHash("hash2");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(44L);
    }
}
