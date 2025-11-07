package saas.personal_branding.api.presentation.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.Map;

@Builder
public record PlatformConnectionResponse(
        Long id,
        Long userId,
        String platformName,
        String externalAccountId,
        String externalUsername,
        String externalDisplayName,
        String status,
        Map<String, Object> metadata,
        Instant lastSyncedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
