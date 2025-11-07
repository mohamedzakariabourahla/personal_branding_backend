package saas.personal_branding.api.infrastructure.mapping;

import saas.personal_branding.api.domain.model.EncryptedSecret;
import saas.personal_branding.api.domain.model.PlatformToken;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformTokenEntity;

import java.util.Arrays;
import java.util.List;

public final class PlatformTokenEntityMapper {

    private PlatformTokenEntityMapper() {
    }

    public static PlatformToken toDomain(PlatformTokenEntity entity) {
        if (entity == null) {
            return null;
        }

        return PlatformToken.builder()
                .id(entity.getId())
                .connectionId(entity.getConnection() != null ? entity.getConnection().getId() : null)
                .accessToken(toSecret(entity.getAccessTokenCipher(), entity.getAccessTokenIv()))
                .refreshToken(toSecret(entity.getRefreshTokenCipher(), entity.getRefreshTokenIv()))
                .tokenType(entity.getTokenType())
                .scopes(toList(entity.getScopes()))
                .accessTokenExpiresAt(entity.getAccessTokenExpiresAt())
                .refreshTokenExpiresAt(entity.getRefreshTokenExpiresAt())
                .lastRotatedAt(entity.getLastRotatedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .fingerprint(entity.getFingerprint())
                .build();
    }

    public static void copyToEntity(PlatformToken source, PlatformTokenEntity target) {
        if (source.getAccessToken() != null) {
            target.setAccessTokenCipher(source.getAccessToken().getCipherText());
            target.setAccessTokenIv(source.getAccessToken().getInitializationVector());
        }
        if (source.getRefreshToken() != null) {
            target.setRefreshTokenCipher(source.getRefreshToken().getCipherText());
            target.setRefreshTokenIv(source.getRefreshToken().getInitializationVector());
        }
        target.setTokenType(source.getTokenType());
        target.setScopes(source.getScopes() != null ? source.getScopes().toArray(String[]::new) : null);
        target.setAccessTokenExpiresAt(source.getAccessTokenExpiresAt());
        target.setRefreshTokenExpiresAt(source.getRefreshTokenExpiresAt());
        target.setLastRotatedAt(source.getLastRotatedAt());
        target.setFingerprint(source.getFingerprint());
    }

    private static EncryptedSecret toSecret(String cipher, byte[] iv) {
        if (cipher == null) {
            return null;
        }
        return EncryptedSecret.builder()
                .cipherText(cipher)
                .initializationVector(iv)
                .build();
    }

    private static List<String> toList(String[] scopes) {
        return scopes == null ? List.of() : List.copyOf(Arrays.asList(scopes));
    }
}
