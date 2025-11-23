package saas.personal_branding.api.infrastructure.mapping;

import saas.personal_branding.api.domain.model.Platform;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformConnectionEntity;
import saas.personal_branding.api.infrastructure.persistence.entity.PlatformEntity;

import java.util.Collections;

public final class PlatformConnectionEntityMapper {

    private PlatformConnectionEntityMapper() {
    }

    public static PlatformConnection toDomain(PlatformConnectionEntity entity) {
        if (entity == null) {
            return null;
        }

        return PlatformConnection.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .platform(toDomainPlatform(entity.getPlatform()))
                .externalAccountId(entity.getExternalAccountId())
                .externalUsername(entity.getExternalUsername())
                .externalDisplayName(entity.getExternalDisplayName())
                .accountMetadata(entity.getAccountMetadata() == null ? Collections.emptyMap() : entity.getAccountMetadata())
                .status(entity.getStatus())
                .lastError(entity.getLastError())
                .lastSyncedAt(entity.getLastSyncedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static void copyToEntity(PlatformConnection source, PlatformConnectionEntity target) {
        target.setExternalAccountId(source.getExternalAccountId());
        target.setExternalUsername(source.getExternalUsername());
        target.setExternalDisplayName(source.getExternalDisplayName());
        target.setAccountMetadata(source.getAccountMetadata());
        target.setStatus(source.getStatus());
        target.setLastError(source.getLastError());
        target.setLastSyncedAt(source.getLastSyncedAt());
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

}
