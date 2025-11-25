package saas.personal_branding.api.auth.presentation.dto.response;

import java.time.Instant;

public record SessionResponse(String deviceId,
                              String deviceName,
                              String userAgent,
                              String ipAddress,
                              Instant createdAt,
                              Instant lastUsedAt,
                              Instant expiresAt) {
}
