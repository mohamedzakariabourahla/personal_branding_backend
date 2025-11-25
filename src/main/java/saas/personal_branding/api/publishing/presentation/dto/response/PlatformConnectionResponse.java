package saas.personal_branding.api.publishing.presentation.dto.response;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;

@Builder
public record PlatformConnectionResponse(Long id,
                                         Long userId,
                                         Long platformId,
                                         String platformCode,
                                         String platformName,
                                         String externalAccountId,
                                         String externalUsername,
                                         String externalDisplayName,
                                         String status,
                                         Map<String, Object> metadata,
                                         Instant lastSyncedAt,
                                         Instant createdAt,
                                         Instant updatedAt) {
}
