package saas.personal_branding.api.publishing.presentation.mapper;

import java.util.Map;
import saas.personal_branding.api.domain.model.PlatformConnection;
import saas.personal_branding.api.publishing.presentation.dto.response.PlatformConnectionResponse;

public final class PlatformConnectionDtoMapper {

    private PlatformConnectionDtoMapper() {
    }

    public static PlatformConnectionResponse toResponse(PlatformConnection connection) {
        Map<String, Object> metadata = connection.getAccountMetadata();
        return PlatformConnectionResponse.builder()
                .id(connection.getId())
                .userId(connection.getUserId())
                .platformId(connection.getPlatform().getId())
                .platformCode(connection.getPlatform().getCode())
                .platformName(connection.getPlatform().getName())
                .externalAccountId(connection.getExternalAccountId())
                .externalUsername(connection.getExternalUsername())
                .externalDisplayName(connection.getExternalDisplayName())
                .status(connection.getStatus() != null ? connection.getStatus().name() : null)
                .metadata(metadata)
                .lastSyncedAt(connection.getLastSyncedAt())
                .createdAt(connection.getCreatedAt())
                .updatedAt(connection.getUpdatedAt())
                .build();
    }
}
