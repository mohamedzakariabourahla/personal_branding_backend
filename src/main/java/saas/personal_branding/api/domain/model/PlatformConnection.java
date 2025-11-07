package saas.personal_branding.api.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
public class PlatformConnection {
    private final Long id;
    private final Long userId;
    private final Platform platform;
    private final String externalAccountId;
    private final String externalUsername;
    private final String externalDisplayName;
    @Singular("metadataEntry")
    private final Map<String, Object> accountMetadata;
    private final PlatformConnectionStatus status;
    private final String lastError;
    private final Instant lastSyncedAt;
    private final Instant createdAt;
    private final Instant updatedAt;
}
