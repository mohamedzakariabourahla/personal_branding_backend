package saas.personal_branding.api.auth.infrastructure.persistence.repositoryAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.personal_branding.api.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import saas.personal_branding.api.auth.infrastructure.persistence.jpa.JpaRefreshTokenRepository;
import saas.personal_branding.api.domain.model.RefreshToken;
import saas.personal_branding.api.infrastructure.persistence.entity.UserEntity;
import saas.personal_branding.api.infrastructure.persistence.jpa.JpaUserRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenRepositoryAdapterTest {

    @Mock
    private JpaRefreshTokenRepository jpaRefreshTokenRepository;
    @Mock
    private JpaUserRepository jpaUserRepository;

    private RefreshTokenRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RefreshTokenRepositoryAdapter(jpaRefreshTokenRepository, jpaUserRepository);
    }

    @Test
    void savePersistsAndMapsBack() {
        UserEntity user = UserEntity.builder().id(5L).email("user@example.com").build();
        when(jpaUserRepository.findById(5L)).thenReturn(Optional.of(user));

        RefreshTokenEntity savedEntity = RefreshTokenEntity.builder()
                .id(10L)
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.parse("2026-01-01T00:00:00Z"))
                .revoked(false)
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .lastUsedAt(Instant.parse("2025-01-01T00:00:00Z"))
                .deviceId("device-1")
                .deviceName("Device")
                .build();
        when(jpaRefreshTokenRepository.save(any())).thenReturn(savedEntity);

        RefreshToken domain = RefreshToken.builder()
                .userId(5L)
                .tokenHash("hash")
                .expiresAt(savedEntity.getExpiresAt())
                .createdAt(savedEntity.getCreatedAt())
                .lastUsedAt(savedEntity.getLastUsedAt())
                .deviceId("device-1")
                .deviceName("Device")
                .revoked(false)
                .build();

        RefreshToken result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUserId()).isEqualTo(5L);
        verify(jpaRefreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void findActiveByTokenHashMaps() {
        UserEntity user = UserEntity.builder().id(5L).email("user@example.com").build();
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .id(10L)
                .user(user)
                .tokenHash("hash")
                .expiresAt(Instant.now())
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .deviceId("device-1")
                .build();
        when(jpaRefreshTokenRepository.findByTokenHashAndRevokedFalse("hash")).thenReturn(Optional.of(entity));

        Optional<RefreshToken> result = adapter.findActiveByTokenHash("hash");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        assertThat(result.get().getUserId()).isEqualTo(5L);
    }

    @Test
    void findActiveByUserIdMapsList() {
        UserEntity user = UserEntity.builder().id(5L).email("user@example.com").build();
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .id(11L)
                .user(user)
                .tokenHash("hash2")
                .expiresAt(Instant.now().plusSeconds(1000))
                .createdAt(Instant.now())
                .lastUsedAt(Instant.now())
                .deviceId("device-2")
                .build();
        when(jpaRefreshTokenRepository.findActiveByUserIdOrderByLastUsedAtDesc(any(), any()))
                .thenReturn(List.of(entity));

        List<RefreshToken> tokens = adapter.findActiveByUserId(5L);

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getId()).isEqualTo(11L);
    }
}
