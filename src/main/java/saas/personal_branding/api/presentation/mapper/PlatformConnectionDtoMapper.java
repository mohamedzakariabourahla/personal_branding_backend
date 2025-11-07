package saas.personal_branding.api.presentation.mapper;

import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.presentation.dto.response.PlatformConnectionResponse;

public final class PlatformConnectionDtoMapper {

    private PlatformConnectionDtoMapper() {
    }

    public static PlatformConnectionResponse toResponse(PlatformConnection connection) {
        return PlatformConnectionResponse.builder()
                .id(connection.getId())
                .userId(connection.getUserId())
                .platformName(connection.getPlatform() != null ? connection.getPlatform().getName() : null)
                .externalAccountId(connection.getExternalAccountId())
                .externalUsername(connection.getExternalUsername())
                .externalDisplayName(connection.getExternalDisplayName())
                .status(connection.getStatus() != null ? connection.getStatus().name() : null)
                .metadata(connection.getAccountMetadata())
                .lastSyncedAt(connection.getLastSyncedAt())
                .createdAt(connection.getCreatedAt())
                .updatedAt(connection.getUpdatedAt())
                .build();
    }
}
