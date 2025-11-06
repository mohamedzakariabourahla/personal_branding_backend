package saas.personal_branding.api.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class RefreshToken {

    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final boolean revoked;
    private final Instant createdAt;
    private final String deviceId;
    private final String deviceName;
    private final String userAgent;
    private final String ipAddress;
    private final Instant lastUsedAt;

    public boolean isExpired(Instant referenceTime) {
        return expiresAt != null && expiresAt.isBefore(referenceTime);
    }
}
