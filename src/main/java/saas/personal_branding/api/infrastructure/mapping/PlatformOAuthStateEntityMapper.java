package saas.personal_branding.api.infrastructure.mapping;

import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformOAuthState;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformOAuthStateEntity;

import java.util.Arrays;
import java.util.List;

public final class PlatformOAuthStateEntityMapper {

    private PlatformOAuthStateEntityMapper() {
    }

    public static PlatformOAuthState toDomain(PlatformOAuthStateEntity entity) {
        if (entity == null) {
            return null;
        }

        return PlatformOAuthState.builder()
                .state(entity.getState())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .platform(toDomainPlatform(entity.getPlatform()))
                .codeVerifier(entity.getCodeVerifier())
                .redirectUri(entity.getRedirectUri())
                .requestedScopes(toList(entity.getRequestedScopes()))
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .build();
    }

    public static void copyToEntity(PlatformOAuthState source, PlatformOAuthStateEntity target) {
        target.setCodeVerifier(source.getCodeVerifier());
        target.setRedirectUri(source.getRedirectUri());
        target.setRequestedScopes(source.getRequestedScopes() != null ? source.getRequestedScopes().toArray(String[]::new) : null);
        target.setCreatedAt(source.getCreatedAt());
        target.setExpiresAt(source.getExpiresAt());
    }

    private static Platform toDomainPlatform(PlatformEntity entity) {
        if (entity == null) {
            return null;
        }
        return Platform.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .build();
    }

    private static List<String> toList(String[] scopes) {
        return scopes == null ? List.of() : List.copyOf(Arrays.asList(scopes));
    }
}
