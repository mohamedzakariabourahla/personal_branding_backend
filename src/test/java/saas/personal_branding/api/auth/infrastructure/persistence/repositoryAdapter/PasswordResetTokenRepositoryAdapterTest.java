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
import saas.personal_branding.api.auth.infrastructure.persistence.entity.PasswordResetTokenEntity;
import saas.personal_branding.api.auth.infrastructure.persistence.jpa.JpaPasswordResetTokenRepository;
import saas.personal_branding.api.domain.model.PasswordResetToken;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenRepositoryAdapterTest {

    @Mock
    private JpaPasswordResetTokenRepository jpaRepository;
    @Mock
    private JpaUserRepository jpaUserRepository;

    private PasswordResetTokenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PasswordResetTokenRepositoryAdapter(jpaRepository, jpaUserRepository);
    }

    @Test
    void savePersistsAndMaps() {
        UserEntity user = UserEntity.builder().id(9L).email("user@example.com").build();
        when(jpaUserRepository.findById(9L)).thenReturn(Optional.of(user));

        PasswordResetTokenEntity saved = PasswordResetTokenEntity.builder()
                .id(55L)
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.parse("2025-02-01T00:00:00Z"))
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();
        when(jpaRepository.save(any())).thenReturn(saved);

        PasswordResetToken token = PasswordResetToken.builder()
                .userId(9L)
                .tokenHash("hash")
                .expiresAt(saved.getExpiresAt())
                .createdAt(saved.getCreatedAt())
                .build();

        PasswordResetToken result = adapter.save(token);

        assertThat(result.getId()).isEqualTo(55L);
        assertThat(result.getUserId()).isEqualTo(9L);
    }

    @Test
    void findByTokenHashMaps() {
        UserEntity user = UserEntity.builder().id(10L).email("user@example.com").build();
        PasswordResetTokenEntity entity = PasswordResetTokenEntity.builder()
                .id(66L)
                .user(user)
                .tokenHash("hash2")
                .expiresAt(Instant.now())
                .createdAt(Instant.now())
                .build();
        when(jpaRepository.findByTokenHash("hash2")).thenReturn(Optional.of(entity));

        Optional<PasswordResetToken> result = adapter.findByTokenHash("hash2");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(66L);
    }
}
